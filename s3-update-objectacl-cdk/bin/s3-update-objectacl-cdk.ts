#!/usr/bin/env node
import * as cdk from 'aws-cdk-lib';
import { S3UpdateObjectaclCdkStack } from '../lib/s3-update-objectacl-cdk-stack';

const app = new cdk.App();
new S3UpdateObjectaclCdkStack(app, 'S3UpdateObjectaclCdkStack');
