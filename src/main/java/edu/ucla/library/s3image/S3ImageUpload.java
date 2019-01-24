
package edu.ucla.library.s3image;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;

import javax.inject.Inject;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import info.freelibrary.util.FileUtils;
import info.freelibrary.util.Logger;
import info.freelibrary.util.LoggerFactory;

import io.airlift.airline.Command;
import io.airlift.airline.HelpOption;
import io.airlift.airline.Option;
import io.airlift.airline.SingleCommand;

/**
 * A throw-away command line program to upload images, with IDs, into an S3 bucket.
 * <p>
 * To use: <code>java -jar target/s3-image-uploader-0.0.1.jar -h</code> For instance: <code>
 *   AWS_PROFILE=jiiifylambda java -jar target/s3-image-uploader-0.0.1.jar \
 *    -b jiiify-tiler-ingest-bucket-us-west-1 -r us-west-1 -m 1 -c input.csv
 * </code>
 * </p>
 */
@Command(name = "edu.ucla.library.s3image.S3ImageUpload", description = "An S3 image uploader")
public final class S3ImageUpload {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3ImageUpload.class, "s3imageup_messages");

    @Inject
    public HelpOption myHelpOption;

    @Option(name = { "-c", "--csv" }, description = "A CSV file with IDs and paths of images to upload")
    public File myCSVFile;

    @Option(name = { "-b", "--bucket" }, description = "A destination S3 bucket")
    public String myDestination;

    @Option(name = { "-m", "--max" }, description = "A maximum number of files to upload")
    public int myMaxCount;

    @Option(name = { "-r", "--region" }, description = "The upload bucket's region")
    public String myRegion;

    /**
     * The main method for the reconciler program.
     *
     * @param args Arguments supplied to the program
     */
    @SuppressWarnings("uncommentedmain")
    public static void main(final String[] args) {
        final S3ImageUpload s3ImageUpload = SingleCommand.singleCommand(S3ImageUpload.class).parse(args);

        if (s3ImageUpload.myHelpOption.showHelpIfRequested()) {
            return;
        }

        s3ImageUpload.run();
    }

    private void run() {
        Objects.requireNonNull(myCSVFile, LOGGER.getMessage(MessageCodes.T_001));
        Objects.requireNonNull(myDestination, LOGGER.getMessage(MessageCodes.T_002));

        final Regions region = myRegion != null ? Regions.fromName(myRegion) : Regions.US_EAST_1;
        final AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard().withRegion(region);
        final AmazonS3 s3Client = builder.build();

        try {
            final CSVParser parser = new CSVParserBuilder().withSeparator(',').withIgnoreQuotations(true).build();
            final CSVReader reader = new CSVReaderBuilder(new FileReader(myCSVFile)).withCSVParser(parser).build();
            final Iterator<String[]> iterator = reader.iterator();

            int count = 0;

            LOGGER.info(MessageCodes.T_004, myCSVFile);

            while (iterator.hasNext()) {
                final String[] image = iterator.next();

                if ((image.length == 2) && (count < myMaxCount)) {
                    final String id = image[0];
                    final String path = image[1];

                    LOGGER.debug(MessageCodes.T_005, id, path);

                    count = upload(id, path, s3Client, count);
                } else if (count >= myMaxCount) {
                    LOGGER.info(MessageCodes.T_003, myMaxCount);
                    break;
                } else if (image.length == 1) {
                    final String path = image[0];
                    final String[] parts = path.split("/");
                    final String id = FileUtils.stripExt(parts[parts.length - 1]);

                    LOGGER.debug(MessageCodes.T_005, id, path);

                    count = upload(id, path, s3Client, count);
                }
            }
        } catch (final IOException details) {
            System.err.println(details);
        }
    }

    private int upload(final String aID, final String aPath, final AmazonS3 aS3Client, final int aCount) {
        try {
            final FileInputStream inStream = new FileInputStream(aPath);
            final ObjectMetadata metadata = new ObjectMetadata();
            final PutObjectRequest req = new PutObjectRequest(myDestination, aPath, inStream, metadata);

            metadata.setContentLength(new File(aPath).length());
            metadata.setContentType(FileUtils.getMimeType(aPath));
            metadata.addUserMetadata("id", aID);

            aS3Client.putObject(req);

            return aCount + 1;
        } catch (final FileNotFoundException details) {
            LOGGER.error(MessageCodes.T_006, aPath);

            // We're not actually throwing an exception, just not counting it as a successful upload
            return aCount;
        }
    }
}
