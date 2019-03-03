// Copyright 2019, Oracle Corporation and/or its affiliates. All rights reserved.

import React, { useState, useEffect } from 'react'
import axios from 'axios'
import { REST_AUTH_HEADER, REST_ADDRESS } from '../shared/Rest'
import { Card, CardBody, CardTitle, CardHeader, Button } from 'reactstrap'
import { Link } from 'react-router-dom'

function RenderDomain({ domain }) {
    return (
        <Card>
            <CardHeader>Domain</CardHeader>
            <Link to={`/domain/${domain.domainUID}`}>
                <CardBody>
                    <CardTitle>{domain.domainUID}</CardTitle>
                </CardBody>
            </Link>
        </Card>
    )
}


export const DomainList = (props) => {

    //
    //  Retrieve the list of domains
    //
    const [domains, setDomains] = useState([])
    useEffect(() => {
        axios(REST_ADDRESS + 'operator/latest/domains', {
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
            .then(domains => setDomains(domains))
            .catch(error => console.log(error.message))
    }, [])


    const domainList = domains.map((domain) => {
        return (
            <div key={domain.domainUID} className="col-12 col-md-5 m-1">
                <RenderDomain domain={domain} />
            </div>
        );
    });

    return (
        <div className="container">
            <p>&nbsp;</p>
            <div className="row align-items-start">
                <div className="col-12 col-md m-1">
                    <p>The following domains are being managed:</p>
                </div>
            </div>
            <div className="row">
                {domainList}
            </div>
            <p>&nbsp;</p>
            <div className="row align-items-start">
                <div className="col-12 col-md m-1">
                    <Button color="primary" size="sm">Create a new domain...</Button>
                </div>
            </div>
        </div>
    )
}
