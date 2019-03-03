// Copyright 2019, Oracle Corporation and/or its affiliates. All rights reserved.

import React, { Component } from 'react'
import { About } from './About'
import { Home } from './Home'
import Header from './Header'
import { Switch, Route, Redirect, withRouter } from 'react-router-dom'
import { fetchOperators } from '../redux/ActionCreators'
import { connect } from 'react-redux'

const mapStateToProps = state => {
    return {
        operators: state.Operators
    }
}

const mapDispatchToProps = (dispatch) => ({
    fetchOperators: () => { dispatch(fetchOperators()) }
})

class Main extends Component {
    componentDidMount() {
        this.props.fetchOperators()
    }

    render() {
        const HomePage = () => {
            return (
                <Home />
            )
        }

        return (
            <div>
                <Header />
                <Switch>
                    <Route path="/home" component={HomePage} />
                    <Route exact path="/about" component={About} />
                    <Redirect to="/home" />
                </Switch>
            </div>
        );
    }
}

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(Main));
