# s3-image-upload

Simple script to upload images from a CSV file to an S3 bucket, setting their IDs as metadata in the process.

How to use:

    AWS_PROFILE="jiiifylambda" java -jar s3-image-uploader-0.0.1.jar -c input.csv -b jiiify-tiler-ingest-bucket

To see what arguments mean:

    AWS_PROFILE="jiiifylambda" java -jar s3-image-uploader-0.0.1.jar -h
