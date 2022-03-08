// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

import { Duration, Stack, StackProps } from 'aws-cdk-lib';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as cdk from 'aws-cdk-lib';

import { Construct } from 'constructs';

export class S3UpdateObjectaclCdkStack extends Stack {
  constructor(scope: Construct, id: string, props?: StackProps) {
    super(scope, id, props);

    const cfOAICanonicalID = new cdk.CfnParameter(this, "cfOAICanonicalID", {
      type: "String",
      allowedPattern: "[a-zA-Z0-9]{64,96}",
      description: "The canonical id of the cloudfront distribution"});
    
      const grantLevel = new cdk.CfnParameter(this, "grantLevel", {
        type: "String",
        allowedValues: ['READ', 'WRITE', 'READ_ACP', 'WRITE_ACP', 'FULL_CONTROL'],
        description: "The grant level to be assigned on the S3 object"});
    
    const s3PutObjectAcl = new lambda.Function(this, 'S3PutObjectAcl', {
      runtime: lambda.Runtime.PYTHON_3_9,
      code: lambda.Code.fromAsset('lambda'),
      handler: 'S3PutObjectACL.lambda_handler',
      environment: {
        CANONICAL_ID: cfOAICanonicalID.valueAsString,
        GRANT_LEVEL: grantLevel.valueAsString
      }
    })

    const s3PutObjectAclPolicy = 
      new iam.PolicyStatement({
      actions: ['s3:GetObject','s3:GetObjectAcl','s3:PutObjectAcl'],
      resources: ['*'],
    });
    
    s3PutObjectAcl.role?.attachInlinePolicy(
      new iam.Policy(this, 'S3PutObjectAclPolicy', {
        statements: [s3PutObjectAclPolicy]
      })
    )

  }
}
