//common helper for invoking SBT tasks
void sbtAction(String task) {
  echo "Executing ${task} on SBT"
  sh '''
      echo "realm=Sonatype Nexus Repository Manager\nhost=${NEXUS}\nuser=${NEXUS_CREDENTIALS_USR}\npassword=${NEXUS_CREDENTIALS_PSW}" > ~/.sbt/.credentials
     '''
  //using both interpolation and string concatenation to avoid Jenkins security warnings
  sh 'sbt -Dsbt.log.noformat=true -Djavax.net.ssl.trustStore=./PDNDTrustStore -Djavax.net.ssl.trustStorePassword=${PDND_TRUST_STORE_PSW} generateCode "project root" ' + "${task}"
}

pipeline {

  agent none

  stages {
    stage('Initializing build') {
      agent { label 'sbt-template' }
      environment {
        PDND_TRUST_STORE_PSW = credentials('pdnd-interop-trust-psw')
      }
      steps {
        withCredentials([file(credentialsId: 'pdnd-interop-trust-cert', variable: 'pdnd_certificate')]) {
          sh '''
             cat \$pdnd_certificate > gateway.interop.pdnd.dev.cer
             keytool -import -file gateway.interop.pdnd.dev.cer -alias pdnd-interop-gateway -keystore PDNDTrustStore -storepass ${PDND_TRUST_STORE_PSW} -noprompt
             cp $JAVA_HOME/jre/lib/security/cacerts main_certs
             keytool -importkeystore -srckeystore main_certs -destkeystore PDNDTrustStore -srcstorepass ${PDND_TRUST_STORE_PSW} -deststorepass ${PDND_TRUST_STORE_PSW}
           '''
          stash includes: "PDNDTrustStore", name: "pdnd_trust_store"
        }
      }
    }

    stage('Test and Deploy µservice') {
      agent { label 'sbt-template' }
      environment {
        NEXUS = "${env.NEXUS}"
        NEXUS_CREDENTIALS = credentials('pdnd-nexus')
        DOCKER_REPO = "${env.DOCKER_REPO}"
        MAVEN_REPO = "${env.MAVEN_REPO}"
        ECR_RW = credentials('ecr-rw')
        PDND_TRUST_STORE_PSW = credentials('pdnd-interop-trust-psw')
      }
      steps {
        container('sbt-container') {
          unstash "pdnd_trust_store"
          script {
            sh '''
            export AWS_ACCESS_KEY_ID=$ECR_RW_USR
            export AWS_SECRET_ACCESS_KEY=$ECR_RW_PSW
            aws ecr get-login-password --region eu-central-1 | docker login --username AWS --password-stdin $DOCKER_REPO
            '''
            sbtAction 'test docker:publish "project client" publish'
          }
        }
      }
    }

    stage('Apply Kubernetes files') {
      agent { label 'sbt-template' }
      environment {
        PDND_INTEROP_KEYS = credentials('pdnd-interop-keys')
        USER_REGISTRY_API_KEY = credentials('userRegistryApiKey')
        MAIN_AUDIENCE = "${env.MAIN_AUDIENCE}"
        DOCKER_REPO = "${env.DOCKER_REPO}"
        REPLICAS_NR = 1
      }
      steps {
        container('sbt-container') {
          withKubeConfig([credentialsId: 'kube-config']) {
            sh '''
              cd kubernetes
              chmod u+x undeploy.sh
              chmod u+x deploy.sh
              ./undeploy.sh
              ./deploy.sh
            '''
          }
        }
      }
    }
  }
}