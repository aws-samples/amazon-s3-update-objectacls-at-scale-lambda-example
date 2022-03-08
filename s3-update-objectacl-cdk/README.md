# Overview
The is a sample CDK to deploy lambda(python) for updating S3 object ACLs that can be triggered via S3 batch operations. 

# Deploy
This demo can be deployed via CDK in your own AWS Account. 

###### Prerequisites
- [aws-cdk](https://docs.aws.amazon.com/cdk/latest/guide/getting_started.html) installed and [bootstrapped](https://docs.aws.amazon.com/cdk/latest/guide/bootstrapping.html)
- npm installed
- aws credentials configured

This CDK expects 2 input parameters during the deployment,
- cfOAICanonicalID(Required) - The CloudFront Origin Access ID's(OAI) CanonicalID to which the S3 object ACL access is needed. This is usually a 96 char long alphanumeric string. The deployment stops if this value is not provided or an invalid string is passed in.
- grantLevel(Required) - The level of access needed for the OAI. Valid values are 'READ', 'WRITE', 'READ_ACP', 'WRITE_ACP', 'FULL_CONTROL'. The deployment stops if this value is not provided or an invalid value is passed in.

```
cdk deploy --parameters cfOAICanonicalID=<CanonicalID> --parameters grantLevel=<READ|WRITE|READ_ACP|WRITE_ACP|FULL_CONTROL>
```

**Note:** The sample code demonstrates how to update grantee based on type ID, but can easily updated to support other types as well, for e.g., URI or EmailAddress

# How it works
Once the CDK is deployed, it creates a lambda function that can subsequently be configured in S3 batch operations. 
Refer to this [blog post](blogpost link) to check out how this lambda can be configured to be triggered during S3 Batch Operations

# Cleanup
To remove all deployed resources
```
cdk destroy
```