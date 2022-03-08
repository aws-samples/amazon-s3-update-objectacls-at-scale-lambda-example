// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package s3updateacl;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3BatchEvent;
import com.amazonaws.services.lambda.runtime.events.S3BatchResponse;
import com.amazonaws.services.lambda.runtime.events.S3BatchResponse.Result;
import com.amazonaws.services.lambda.runtime.events.S3BatchResponse.ResultCode;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.CanonicalGrantee;
import com.amazonaws.services.s3.model.Permission;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<S3BatchEvent, Object> {

    public Object handleRequest(final S3BatchEvent s3BatchEvent, final Context context) {

        LambdaLogger logger = context.getLogger();
        String invocationId = null, invocationSchemaVersion = null, canonicalID = null,  taskId = null;

        try {
            invocationId = s3BatchEvent.getInvocationId();
            invocationSchemaVersion = s3BatchEvent.getInvocationSchemaVersion();
            canonicalID = System.getenv("CanonicalID");
            String permissionString = System.getenv("Permission");

            Permission permission = null;

            switch(permissionString) {
                case "READ_ACP": 
                    permission = Permission.ReadAcp; break;
                case "WRITE_ACP": 
                    permission = Permission.WriteAcp; break;
                case "READ":
                    permission = Permission.Read; break;
                case "WRITE":
                    permission = Permission.Write; break;
                default:
                    permission = Permission.FullControl;  
            }

            S3BatchEvent.Task task = s3BatchEvent.getTasks().get(0);
            taskId = task.getTaskId();
            String s3Key = URLDecoder.decode(task.getS3Key(), StandardCharsets.UTF_8);
            String s3VersionId = task.getS3VersionId();
            String bucketName = task.getS3BucketArn().substring(task.getS3BucketArn().lastIndexOf(":")+1);

            logger.log("Received task with s3Key : " + s3Key + " s3VersionId: " + s3VersionId + " bucketName : " + bucketName);

            final AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(System.getenv("AWS_REGION")).build();

            // Get the existing object ACL that needs to be modified.
            AccessControlList acl = s3Client.getObjectAcl(bucketName, s3Key);

            // Grant the necessary permissions.
            acl.grantPermission(new CanonicalGrantee(canonicalID), permission);

            // Save the modified ACL back to the object.
            s3Client.setObjectAcl(bucketName, s3Key, acl);

            Result result = new Result(taskId, ResultCode.Succeeded, "Successfully updated") ;
            List<Result> results = new ArrayList<Result>();
            results.add(result);
            return new S3BatchResponse(invocationSchemaVersion, ResultCode.Succeeded, invocationId, results);
        } catch (Exception e) {
            Result result = new Result(taskId, ResultCode.PermanentFailure, "Failed to update " + e.getMessage());
            List<Result> results = new ArrayList<Result>();
            results.add(result);
            return new S3BatchResponse(invocationSchemaVersion, ResultCode.PermanentFailure, invocationId, results);
        }
    }
}
