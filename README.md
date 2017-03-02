# srx-services-sre
**Transfers SRE XML from District to District.**

***

## Configuration

### Environment Variables
Variable | Description | Example
-------- | ----------- | -------
AES_PASSWORD | Password used to decrypt AES encrypted request body payloads. |  (see Heroku)
AES_SALT | Salt used to decrypt AES encrypted request body payloads. | (see Heroku)
AMAZON_S3_ACCESS_KEY | AWS S3 access key for SRX cache. | (see Heroku)
AMAZON_S3_BUCKET_NAME | AWS S3 bucket name for SRX cache. | (see Heroku)
AMAZON_S3_PATH | Root path to files within SRX cache. | (see Heroku)
AMAZON_S3_SECRET | AWS S3 secret for SRX cache. | (see Heroku)
AMAZON_S3_TIMEOUT | AWS S3 request timeout in ms. | 300000
ENVIRONMENT | Deployment environment name.  | development
LOG_LEVEL | Logging level (info, debug, error). | debug
ROLLBAR_ACCESS_TOKEN | Rollbar access token for error logging. | (see Heroku)
ROLLBAR_URL | URL to Rollbar API. | https://api.rollbar.com/api/1/item/
SERVER_API_ROOT | Root path for this service. | (typically leave blank)
SERVER_HOST  | Host IP for this service. | 127.0.0.1
SERVER_NAME | Server name for this service. | localhost
SERVER_PORT | Port this service listens on. | 8080
SERVER_URL | URL for this service. | http://localhost
SRX_ENVIRONMENT_URL | HostedZone environment URL. | https://psesd.hostedzone.com/svcs/dev/requestProvider
SRX_SESSION_TOKEN | HostedZone session token assigned to this service. | (see HostedZone configuration)
SRX_SHARED_SECRET  | HostedZone shared secret assigned to this service. | (see HostedZone configuration)

### HostedZone
The SRE service (srx-services-sre) must be registered in HostedZone as a new "environment" (application) that provides the following "services" (resources):

 * sres

Once registered, the supplied HostedZone session token and shared secret should be set in the srx-services-sres host server (Heroku) environment variables (see above).

This SRE service must be further configured in HostedZone as follows:

Service | Zone | Context | Provide | Query | Create | Update | Delete
------- | ---- | ------- | ------- | ----- | ------ | ------ | ------
sres | default | default | X | | | | |
sres | [district*] | transfer | X | | | | |
sres | test | default | X | | | | |
sres | test | test | X | | | | |
sres | test | transfer | X | | | | |
srxMessages | default | default |   | |X | | |
srxMessages | [district*] | default |   | |X | | |
srxMessages | test | default |   | |X | | |
srxMessages | test | test |   | |X | | |
srxZoneConfig | default | default |   | X| | | |
srxZoneConfig | [district*] | default |   | X| | | |
srxZoneConfig | test | default |   | X| | | |
srxZoneConfig | test | test |   | X| | | |

[district*] = all district zones utilizing SRX services

## Usage

### SREs
SREs are submitted via a POST request using the following URL format:

```
https://[baseUrl]/sres;zoneId=[zoneId];contextId=[contextId]
```

Variable | Description | Example
--------- | ----------- | -------
baseUrl   | URL of the deployment environment hosting the adapter endpoints. |  srx-services-sre-dev.herokuapp.com
zoneId    | Zone containing the requested student SRE record | seattle
contextId | Client context of request. | CBO


The following required headers must be present in the POST request:

Header | Description | Example
------ | ----------- | -------
authorization | Must be set to a valid HMAC-SHA256 encrypted authorization token | SIF_HMACSHA256 ZGNlYjgxZmQtNjE5My00NWVkL...
timeStamp | Must be set to a valid date/time in the following format: yyyy-MM-ddTHH:mm:ss:SSSZ | 2016-12-20T18:09:18.539Z
x-psesd-iv | Must be set to a valid PSESD iv value exactly 16 characters in length | g123j894w479q712

The following optional headers may also be included:

Header | Description | Example
------ | ----------- | -------
generatorId | Identification token of the “generator” of this request or event | testgenerator
messageId | Consumer-generated. If specified, must be set to a valid UUID | ba74efac-94c1-42bf-af8b-9b149d067816
messageType | If specified, must be set to: REQUEST | REQUEST
requestAction | If specified, must be set to: CREATE | CREATE
requestId | Consumer-generated. If specified, must be set to a valid UUID | ba74efac-94c1-42bf-af8b-9b149d067816
requestType | Must be set to DELAYED or IMMEDIATE. Defaults to IMMEDIATE. | IMMEDIATE
serviceType | If specified, must be set to: OBJECT | OBJECT

In addition, the request body must contain an AES encrypted payload representing the SRE file being submitted.

#### Example SRE POST request
```
POST
https://srx-services-sre-dev.herokuapp.com/sres;zoneId=test;contextId=test

authorization: SIF_HMACSHA256 ZGNlYjgxZmQtNjE5My00NWVkL...
timestamp: 2016-12-20T18:09:18.539Z
generatorId: testgenerator
x-forwarded-proto: https
x-forwarded-port: 443
X-PSESD-IV: g123j894w479q712

PGrqNBsNfu4FgbefDcm/Jt/kua9LKkuJkih0LYJjzbFUo11EnrKz8cHih3/aYhv3rHUlgxFU7CA6zoIYE8TTFOnoodRc2ca1TsaEKFscaAk=
```

***
#### Example SRE POST response
```
Content-Type: application/xml; charset=UTF-8
Date: Tue, 20 Dec 2016 18:09:23 UTC
messageId: dcf5d63d-5d07-4b6b-a985-6ca3b6514d1a
messageType: RESPONSE
Responseaction: CREATE
Timestamp: 2016-12-20T18:09:20.468Z

<createResponse>
  <creates>
    <create id="999" advisoryId="1" statusCode="201"/>
  </creates>
</createResponse>
```
