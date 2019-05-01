#!/bin/bash
#
#  Copyright 2019, Oracle Corporation and/or its affiliates. All rights reserved.
#

# configure yum
echo 'Configuring yum...'
yum-config-manager --enable ol7_addons
yum-config-manager --disable ol7_preview
yum-config-manager --disable ol7_developer
yum-config-manager --disable ol7_developer_EPEL

# install docker
echo 'Installing Docker...'
yum -y install docker-engine

# start docker
systemctl enable docker
systemctl start docker

# authenticate to container registry
echo 'Authenticating to container registry...'
docker login container-registry.oracle.com -u weblogicx_ww@oracle.com -p Env1r0nm3nt

# setup firewall
echo 'Configuring firewall...'
firewall-offline-cmd --add-masquerade
firewall-offline-cmd --add-port=10250/tcp
firewall-offline-cmd --add-port=8472/udp
firewall-offline-cmd --add-port=6443/tcp
systemctl restart firewalld

# configure selinux
echo 'Configuring SELinux...'
/usr/sbin/setenforce 0
sed -i -e 's/SELINUX=enforcing/SELINUX=permissive/g' /etc/selinux/config

# install kubeadm
echo 'Installing kubeadm...'
yum -y install kubeadm

# create the cluster
echo 'Creating cluster...'
/sbin/iptables -P FORWARD ACCEPT
/etc/init.d/iptables iptables save
kubeadm-setup.sh up

# validate
echo 'Validating install...'
export KUBECONFIG=/etc/kubernetes/admin.conf
kubectl get nodes -o wide

# copy the kubeconfig file somewhere we can get it from
mkdir -p /home/opc/.kube
cp -i /etc/kubernetes/admin.conf /home/opc/.kube/config
chown opc:opc /home/opc/.kube/config

# let terraform know we are done
touch /tmp/cloud-init-done