// Copyright 2019, Oracle Corporation and/or its affiliates. All rights reserved.

import * as ActionTypes from './ActionTypes'

export const Operators = (state = {
    isLoading: true,
    errMess: null,
    operators: []
}, action) => {
    switch (action.type) {
        case ActionTypes.ADD_OPERATORS:
            return { ...state, isLoading: false, errMess: null, operators: action.payload }
        case ActionTypes.OPERATORS_LOADING:
            return { ...state, isLoading: true, errMess: null, operators: [] }
        case ActionTypes.OPERATORS_FAILED:
            return { ...state, isLoading: false, errMess: action.payload, operators: [] }
        default:
            return state
    }
}