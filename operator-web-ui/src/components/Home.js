// Copyright 2019, Oracle Corporation and/or its affiliates. All rights reserved.

import React, { useState, useEffect } from 'react'
import axios from 'axios'
import { REST_AUTH_HEADER, REST_ADDRESS } from '../shared/Rest'
import { Card, CardBody, CardTitle, CardHeader, Button } from 'reactstrap'
import { Link } from 'react-router-dom'

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


    return (
        <div className="container">
            <p>&nbsp;</p>
            <div className="row align-items-start">
                <div className="col-12 col-md m-1">
                    <Card>
                        <CardHeader>About the Operator</CardHeader>
                        <CardBody>
                            <CardTitle>See details about this operator.</CardTitle>
                            <Link to="/about"><Button color="primary" size="sm">About...</Button></Link>
                        </CardBody>
                    </Card>
                </div>
                <div className="col-12 col-md m-1">
                    <Card>
                        <CardHeader>Domains</CardHeader>
                        <CardBody>
                            <CardTitle>View a list of domain that are being managed by the operator.</CardTitle>
                            <Link to="/domains"><Button color="primary" size="sm">View domains</Button></Link>
                        </CardBody>
                    </Card>
                </div>
                <div className="col-12 col-md m-1">
                    <Card>
                        <CardHeader>Documentation</CardHeader>
                        <CardBody>
                            <CardTitle>Read the documentation for the operator.</CardTitle>
                            <a href="https://oracle.github.io/weblogic-kubernetes-operator"><Button color="primary" size="sm">Read documentation</Button></a>
                        </CardBody>
                    </Card>
                </div>
                <div className="col-12 col-md m-1">
                    <Card>
                        <CardHeader>GitHub Project</CardHeader>
                        <CardBody>
                            <CardTitle>Visit our project on GitHub to learn more about the operator.</CardTitle>
                            <a href="https://github.com/oracle/weblogic-kubernetes-operator"><Button color="primary" size="sm">Visit project</Button></a>
                        </CardBody>
                    </Card>
                </div>
            </div>
            <p>&nbsp;</p>
            <div className="row align-items-start">
                <div className="col-12 col-md m-1">
                    <Card>
                        <CardHeader>Support</CardHeader>
                        <CardBody>
                            <CardTitle>The Oracle WebLogic Server Kuberentes Operator is an open source project, but WebLogic Server customers
                                with a support entitlement can call or visit My Oracle Support to log a Service Request.  If you prefer, you
                                can some and speak directly to the developers on our public slack channel, or log an issue in our GitHub project.
                            </CardTitle>
                            <a href="https://weblogic-slack-inviter.herokuapp.com/"><Button color="primary" size="sm">Get invite</Button></a>
                        </CardBody>
                    </Card>
                </div>
            </div>
        </div>
    )
}
