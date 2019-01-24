# s3-image-upload

Simple script to upload images from a CSV file to an S3 bucket, setting their IDs as metadata in the process.

How to use:

    AWS_PROFILE="jiiifylambda" java -jar target/s3-image-uploader-0.0.1.jar \
      -c input.csv -b jiiify-tiler-ingest-bucket -m 1

or

    AWS_PROFILE="jiiifylambda" java -jar target/s3-image-uploader-0.0.1.jar \
      -c input.csv -b lambda-function-bucket-us-west-1-ucla -r us-west-1 -m 1

To see what arguments mean:

    AWS_PROFILE="jiiifylambda" java -jar target/s3-image-uploader-0.0.1.jar -h

Region does not need to be supplied unless the ingest bucket's region is something other than the default of `us-east-1`.
