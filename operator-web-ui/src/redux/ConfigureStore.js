// Copyright 2019, Oracle Corporation and/or its affiliates. All rights reserved.

import { createStore, combineReducers, applyMiddleware } from 'redux'
import { Operators } from './Operators'
import thunk from 'redux-thunk'
import logger from 'redux-logger'

export const ConfigureStore = () => {
    const store = createStore(
        combineReducers({
            operators: Operators
        }),
        applyMiddleware(thunk, logger)
    )
    return store
}

