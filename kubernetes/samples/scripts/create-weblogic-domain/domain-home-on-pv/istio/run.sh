#!/bin/bash

#export GATEWAY_IP=slc09xwj.us.oracle.com:31380

test_lb() {
    if [[ -z "${GATEWAY_URL}" ]]; then
        echo "missing env GATEWAY_URL"
        return -1
    fi

    number=30
    svc="managed-server"

    # list the pods
    str=$(kubectl get pod | grep $svc | awk '{ print $1 }')
    str1=${str// / }
    pod_names=($str1)
    pod_number=${#pod_names[@]}

    # pod_counts is to calculate how many requests each pod handled
    declare -a pod_counts
    for ((j=0;j<$pod_number;j++))
    do
        pod_counts[$j]=0
    done

    echo "sending $number requests to $GATEWAY_URL/testwebapp/ ..."
    for ((i=0;i<$number;i++))
    do
        content=$(curl http://$GATEWAY_URL/testwebapp/ 2> /dev/null)
        for ((j=0;j<$pod_number;j++))
        do
            echo $content | grep ${pod_names[j]} > /dev/null
            if [ $? -eq 0 ]; then
                pod_counts[j]=$((pod_counts[j]+1))
            fi
        done
    done
    echo " "

    for ((i=0;i<${#pod_counts[@]};i++))
    do
        echo "the access count of pod ${pod_names[i]} is ${pod_counts[i]}"
    done

    return 0
}


test_lb

