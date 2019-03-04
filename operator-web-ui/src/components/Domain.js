// Copyright 2019, Oracle Corporation and/or its affiliates. All rights reserved.

import React, { useState, useEffect } from 'react'
import axios from 'axios'
import { REST_AUTH_HEADER, REST_ADDRESS } from '../shared/Rest'
import { Card, CardBody, CardHeader, CardText } from 'reactstrap'

export const Domain = ({ domainUID }) => {

    //
    //  Retrieve the domain
    //
    const [domain, setDomain] = useState({})
    useEffect(() => {
        axios(REST_ADDRESS + 'operator/latest/domains/' + domainUID, {
            method: 'get',
            headers: {
                'Authorization': REST_AUTH_HEADER,
                'Accept': 'application/json'
            },
            withCredentials: false
        })
            .then(response => {
                console.log(response)
                if (response.status === 200) {
                    return response
                } else {
                    var error = new Error('Error ' + response.status + ': ' + response.statusText)
                    error.response = response
                    throw error
                }
            }, error => {
                var errmess = new Error(error.message)
                throw errmess
            })
            .then(response => response.data)
            .then(domain => setDomain(domain))
            .catch(error => console.log(error.message))
    }, [])

    //
    //  Retrieve list of clusters in this domain
    //
    const [clusters, setClusters] = useState([])
    useEffect(() => {
        axios(REST_ADDRESS + 'operator/latest/domains/' + domainUID + '/clusters', {
            method: 'get',
            headers: {
                'Authorization': REST_AUTH_HEADER,
                'Accept': 'application/json'
            },
            withCredentials: false
        })
            .then(response => {
                console.log(response)
                if (response.status === 200) {
                    return response
                } else {
                    var error = new Error('Error ' + response.status + ': ' + response.statusText)
                    error.response = response
                    throw error
                }
            }, error => {
                var errmess = new Error(error.message)
                throw errmess
            })
            .then(response => response.data.items)
            .then(clusters => setClusters(clusters))
            .catch(error => console.log(error.message))
    }, [])

    //
    // render a list of clusters in this domain
    //
    const clusterList = clusters.map((cluster) => {
        return (
            <div key={cluster.cluster} className="col-12">
                <li>{cluster.cluster}</li>
            </div>
        );
    });

    return (
        <div className="container">
            <p>&nbsp;</p>
            <div className="row align-items-start">
                <div className="col-12 col-md m-1">
                    <p>About the domain {domainUID}</p>
                </div>
            </div>
            <div className="row">
                <Card>
                    <CardHeader>Domain "{domain.domainUID}"</CardHeader>
                    <CardBody>
                        <CardText>Contains the following clusters:</CardText>
                        {clusterList}
                    </CardBody>
                </Card>
            </div>

        </div>
    )
}
