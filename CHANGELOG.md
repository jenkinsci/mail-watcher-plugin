## Changelog

### 1.12

- Optimization: Do not compose the message when there are no recipients ([JENKINS-27503](https://issues.jenkins-ci.org/browse/JENKINS-27503))

### 1.11

- Correctly identify node and initiator of computer idle event.

### 1.10

- Avoid assertion error thrown during maven build ([JENKINS-28888](https://issues.jenkins-ci.org/browse/JENKINS-28888))

### 1.8

- JENKINS-23496: Notify user that put slave temporarily offline that it has become idle
- JENKINS-23555: Fix NPE when no Reply-To address configured

### 1.7

- JENKINS-23482: Globally configured ReplyTo header is not sent
- JENKINS-20538: No notification for master node going offline (effectively fixed once [\#1293](https://github.com/jenkinsci/jenkins/pull/1293) is merged to core)

### 1.6

- JENKINS-20535: Subject for bring a node back online is misleading
- JENKINS-23174: Links for jobs within folders are invalid

### 1.5

- User identity tracked in 'Initiator' field
- Links to job config history diffs included in messages when plugin installed.
