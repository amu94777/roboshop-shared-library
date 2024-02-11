// Declarative //
def call(Map configMap){
pipeline {
    agent {
        node {
            label 'AGENT-1'
        }
    }
     options {
                timeout(time: 1, unit: 'HOURS')
                disableConcurrentBuilds() 
            }
     environment { 
        packageVersion = ''
        //nexusURL = '172.31.13.69:8081'
        
    }
    parameters {
        // string(name: 'PERSON', defaultValue: 'Mr Jenkins', description: 'Who should I say hello to?')

        // text(name: 'BIOGRAPHY', defaultValue: '', description: 'Enter some information about the person')

        booleanParam(name: 'Deploy', defaultValue: 'false', description: 'Toggle this value')

        //choice(name: 'CHOICE', choices: ['One', 'Two', 'Three'], description: 'Pick something')

        //password(name: 'PASSWORD', defaultValue: 'SECRET', description: 'Enter a password')
    }
    stages {
        stage('get the version') {
            steps {
                script {
                      def packageJson = readJSON file: 'package.json'
                      packageVersion = packageJson.version
                      echo "the application version is :$packageVersion"
                }
            }
        }
    

        stage('install dependencies') {
            steps {
                sh """
                    npm install
                """
                }
            }
            
        stage('unit testing') {
            steps {
                sh """
                    echo "here we will do unit testing"
                """
                }
            }
            
        // stage('scanning') {
        //     steps {
        //         sh """
        //             sonar-scanner 
        //         """
        //         }
        //     }

    
        stage('build') {                      ////here we have to zip the files thst is schema,package.jsos
            steps {                                 /// server.js//zip the file and floders..catalogue.zip is the 
                                                      ///zip file and i exclude .git and .zip file within that files
                sh """                                   
                    ls -al 
                    zip -r -q ${configMap.component}.zip ./* -x ".git" - x "*.zip"  
                    ls -ltr                          
                """                                         ///-q is used for hide the logs in console
                }                    
            }
    

         stage('publish artifact') {
            steps {
                nexusArtifactUploader(                            //publish artifacts here
                            nexusVersion: 'nexus3',
                            protocol: 'http',
                            nexusUrl: pipelineGlobals.nexusURL(),
                            groupId: 'com.roboshop',
                            version: "${packageVersion}",         
                            repository: "${configMap.component}",
                            credentialsId: 'nexus-auth',
                            artifacts: [
                                [artifactId: "${configMap.component}",
                                classifier: '',
                                file: "${configMap.component}.zip",
                                type: 'zip']
                            ]
     )
            }

    }
      stage('Deploy') {
        when {
            expression {
                params.Deploy == 'true'
            }
        }
        steps {
            script {
                def params = [
                    string(name: 'version', value: "$packageVersion"),    ///we have to pass version and env parameter to catalogue-deploy-1 pipeline
                    string(name: 'environment', value: "dev")              ////catalogue-1 is upsteram job,it uploads the artifactory to nexus it trigger cataloue-deploy-1 job(downstream job)
                ]
                build job: "${configMap.component}-deploy-1", wait: true, parameters: params
            }
        }
    }
    
    }  


 post { 
        always { 
            echo 'I will always say Hello again!'
            deleteDir()
        }
        failure { 
            echo 'this runs when pipeline is failed, used generally to send some alerts'
        }
        success{
            echo 'I will say Hello when pipeline is success'
        }
    }
}
}