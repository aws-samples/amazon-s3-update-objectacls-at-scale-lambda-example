import logging
from urllib import parse
import boto3
import os

logger = logging.getLogger(__name__)
logger.setLevel('INFO')

s3 = boto3.resource('s3')

def lambda_handler(event, context):
    
    canonical_id = os.getenv("CANONICAL_ID")
    grant_level = os.getenv("GRANT_LEVEL")

    # The permission that needs to be added to the object
    grant_level_entry = {
        "Grantee": {
            "ID": canonical_id,
            "Type": "CanonicalUser"
        },
        "Permission": grant_level
    }

    # Parse job parameters from Amazon S3 batch operations
    invocation_id = event['invocationId']
    invocation_schema_version = event['invocationSchemaVersion']
    
    results = []
    result_code = None
    result_string = None
    bucket_acl_response = None

    task = event['tasks'][0]
    task_id = task['taskId']

    bucket_name = task['s3BucketArn'].split(':')[-1]

    obj_key = parse.unquote(task['s3Key'], encoding='utf-8')
    obj_version_id = task['s3VersionId']

    logger.info("Got task: ObjectACL %s from object %s in bucket %s.", obj_version_id, obj_key, bucket_name)
    object_acl = s3.ObjectAcl(bucket_name, obj_key)
    logger.info("ObjectACL is:  %s", object_acl)
    obj_owner_name = object_acl.owner['DisplayName']
    obj_owner_id = object_acl.owner['ID']
    
    # Append the new permission to the list of the existing permissions
    object_acl.grants.append(grant_level_entry)

    try: 
        # Put the entire list of permissions back to object ACL
        bucket_acl_response = object_acl.put(
            AccessControlPolicy={
            "Grants": object_acl.grants,
            "Owner": {
                "DisplayName": obj_owner_name,
                "ID": obj_owner_id
            }
        })
    except:
        logger.info("[FAILED] - %s", bucket_acl_response)
        result_code="PermanentFailure"
        result_string='["Failed to update ' + obj_key + '"]'

        results.append({
            'taskId': task_id,
            'resultCode': result_code,
            'resultString': result_string
        })

        return {
            'invocationSchemaVersion': invocation_schema_version,
            'treatMissingKeysAs': result_code,
            'invocationId': invocation_id,
            'results': results
        }



    logger.info("updateResponse: %s", bucket_acl_response)
    if bucket_acl_response["ResponseMetadata"]["HTTPStatusCode"] == 200:
        logger.info("[SUCCESS]")
        result_code="Succeeded"
        result_string='["Successfully updated ' + obj_key + '"]'

    else:
        logger.info("[FAILED] - %s", bucket_acl_response)
        result_code="PermanentFailure"
        result_string='["Failed to update ' + obj_key + '"]'
        
    results.append({
        'taskId': task_id,
        'resultCode': result_code,
        'resultString': result_string
    })

    return {
        'invocationSchemaVersion': invocation_schema_version,
        'treatMissingKeysAs': result_code,
        'invocationId': invocation_id,
        'results': results
    }
