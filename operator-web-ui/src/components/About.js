// Copyright 2019, Oracle Corporation and/or its affiliates. All rights reserved.

import React from 'react'
import { Card, CardBody, CardTitle, CardText } from 'reactstrap'

export const About = (props) => {
    return (
        <div className="container">
            <div className="row align-items-start">
                <div className="col-12 col-md m-1">
                    <p>Something else will go here!</p>
                </div>
                <div className="col-12 col-md-5 m-1">
                    <Card>
                        <CardBody>
                            <CardTitle>Card Title</CardTitle>
                            <CardText>Card Text</CardText>
                        </CardBody>
                    </Card>
                </div>
            </div>
        </div>
    )
}
