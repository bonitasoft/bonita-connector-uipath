timestamps {
    ansiColor('xterm') {
        node {
            stage('Setup') {
                checkout scm
            }

            stage('Build') {
                try {
                    if(env.BRANCH_NAME.equals('master')){
                      sh "./mvnw -B clean deploy -DaltDeploymentRepository=${env.ALT_DEPLOYMENT_REPOSITORY_SNAPSHOTS}"  
                    }else{
                       sh './mvnw -B clean verify'
                    }
                    archiveArtifacts 'target/bonita-connector-uipath-*.zip'
                } finally {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
    }
}
