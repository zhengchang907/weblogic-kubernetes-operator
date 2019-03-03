// Copyright 2019, Oracle Corporation and/or its affiliates. All rights reserved.

import React, { useState, useEffect } from 'react'
import axios from 'axios'
import { REST_AUTH_HEADER, REST_ADDRESS } from '../shared/Rest'
import { Card, CardImg, CardImgOverlay, CardTitle } from 'reactstrap'
import { Link } from 'react-router-dom'

function RenderDomain({ domain }) {
    return (
        <Card>
            <Link to={`/domain/${domain.domainUID}`}>
                <CardImg width="100%" />
                <CardImgOverlay>
                    <CardTitle>{domain.domainUID}</CardTitle>
                </CardImgOverlay>
            </Link>
        </Card>
    )
}


export const Home = (props) => {

    //
    //  Retrieve information about the operator itself
    //
    const [operators, setOperators] = useState([])
    useEffect(() => {
        axios(REST_ADDRESS + 'operator', {
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
            .then(response => response.data.items[0])
            .then(operators => setOperators(operators))
            .catch(error => console.log(error.message))
    }, [])

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
            <div className="row align-items-start">
                <div className="col-12 col-md m-1">
                    <p>This operator support REST API version: {operators.version}</p>
                    <p>&nbsp;</p>
                    <p>The following domains are being managed:</p>
                    {domainList}
                </div>
            </div>
        </div>
    )
}
