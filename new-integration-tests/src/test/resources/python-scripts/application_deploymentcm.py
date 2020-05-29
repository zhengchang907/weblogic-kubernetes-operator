# Copyright (c) 2020, Oracle Corporation and/or its affiliates.
# Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

import sys, os, traceback, base64

script_name = 'application_deploymentcm.py'
t3url = "t3://" + admin_host + ":" + admin_port
archive_name = os.path.basename(node_archive_path)
application_name = os.path.basename(node_archive_path).split('.')[0]

def usage():
  print 'Call script as: '
  print 'wlst.sh ' + script_name + ' -skipWLSModuleScanning -loadProperties domain.properties'

def deploy_application():
  try:
    print 'connecting to the admin server'
    connect(admin_username, admin_password, t3url)
    print 'deploying...'
    deploy(application_name, archive_name, targets, remote='true', upload='true')
    print 'done with deployment'
    disconnect()
  except NameError, e:
    print('Apparently properties not set.')
    print('Please check the property: ', sys.exc_info()[0], sys.exc_info()[1])
    usage()
    exit(exitcode=1)
  except:
    print 'Deployment failed'
    print dumpStack()
    apply(traceback.print_exception, sys.exc_info())
    exit(exitcode=1)
 
if __name__== "main":  
  print "Running deploy using user: " + admin_username + " password: " + admin_password + " t3url: " + t3url \
      + " application name : "+ application_name + " archive: " + node_archive_path + " targets: " + targets
  with open(node_archive_path, 'rb') as encoded_archive:
    with open(archive_name, 'wb') as decoded_archive:
      decoded_archive.write(base64.decodebytes(encoded_archive.read()))
  deploy_application()
  exit()
