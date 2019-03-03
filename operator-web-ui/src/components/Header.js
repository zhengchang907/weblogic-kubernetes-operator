// Copyright 2019, Oracle Corporation and/or its affiliates. All rights reserved

import React, { Component } from 'react'
import {
    Navbar, NavbarBrand, Nav, NavbarToggler, Collapse, NavItem, Jumbotron, NavLink
} from 'reactstrap'

class Header extends Component {
    constructor(props) {
        super(props)

        this.state = {
            isNavOpen: false
        }
        this.toggleNav = this.toggleNav.bind(this);
    }

    toggleNav() {
        this.setState({
            isNavOpen: !this.state.isNavOpen
        })
    }

    render() {
        return (
            <React.Fragment>
                <Navbar dark expand="md bg-dark">
                    <div className="container">
                        <NavbarToggler onClick={this.toggleNav} />
                        <NavbarBrand className="mr-auto" href="/">
                            <img src="assets/images/weblogic.png" height="90" width="90" alt="WebLogic Operator" />
                        </NavbarBrand>
                        <Collapse isOpen={this.state.isNavOpen} navbar>
                            <Nav navbar>
                                <NavItem>
                                    <NavLink className="nav-link" href="/home">
                                        <span className="fa fa-home fa-lg" /> Home
                                    </NavLink>
                                </NavItem>
                                <NavItem>
                                    <NavLink className="nav-link" href="/about">
                                        <span className="fa fa-info fa-lg" /> About
                                    </NavLink>
                                </NavItem>
                            </Nav>
                        </Collapse>
                    </div>
                </Navbar>
                <Jumbotron>
                    <div className="container">
                        <div className="row row-header">
                            <div className="col-12">
                                <h1>Oracle WebLogic Server Kubernetes Operator</h1>
                                <p>One of the world's <i>great</i> operators!</p>
                            </div>
                        </div>
                    </div>
                </Jumbotron>
            </React.Fragment>
        )
    }
}

export default Header