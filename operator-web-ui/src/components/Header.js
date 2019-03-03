// Copyright 2019, Oracle Corporation and/or its affiliates. All rights reserved

import React, { useState } from 'react'
import {
    Navbar, NavbarBrand, Nav, NavbarToggler, Collapse, NavItem, NavLink
} from 'reactstrap'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'


export const Header = (props) => {

    const [isNavOpen, setIsNavOpen] = useState(false)

    return (
        <React.Fragment>
            <Navbar dark expand="md bg-dark">
                <div className="container">
                    <NavbarToggler onClick={() => setIsNavOpen(!isNavOpen)} />
                    <NavbarBrand className="mr-auto" href="/">
                        <img src="assets/images/weblogic.png" height="90" width="90" alt="WebLogic" />
                        Kubernetes Operator
                    </NavbarBrand>
                    <Collapse isOpen={isNavOpen} navbar>
                        <Nav navbar className="ml-auto">
                            <NavItem>
                                <NavLink className="nav-link" href="/home">
                                    <FontAwesomeIcon icon="home" /> Home
                                    </NavLink>
                            </NavItem>
                            <NavItem>
                                <NavLink className="nav-link" href="/domains">
                                    <FontAwesomeIcon icon="globe" /> Domains
                                    </NavLink>
                            </NavItem>
                            <NavItem>
                                <NavLink className="nav-link" href="/about">
                                    <FontAwesomeIcon icon="info" /> About
                                    </NavLink>
                            </NavItem>
                        </Nav>
                    </Collapse>
                </div>
            </Navbar>
        </React.Fragment>
    )

}

export default Header