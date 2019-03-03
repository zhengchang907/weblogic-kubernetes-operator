import * as ActionTypes from './ActionTypes'
import { REST_ADDRESS, REST_AUTH_HEADER } from '../shared/Rest'
// import https from 'https'
import axios from 'axios'

// operators

export const fetchOperators = () => (dispatch) => {
    dispatch(operatorsLoading(true))

    return axios(REST_ADDRESS + 'operator', {
        method: 'get',
        //mode: 'no-cors',
        headers: {
            'Authorization': REST_AUTH_HEADER,
            'Accept': 'application/json'
        },
        withCredentials: false
    })
        .then(response => {
            if (response.ok) {
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
        .then(response => response.json())
        .then(operators => dispatch(addOperators(operators)))
        .catch(error => dispatch(operatorsFailed(error.message)))
}

export const operatorsLoading = () => ({
    type: ActionTypes.OPERATORS_LOADING
})

export const operatorsFailed = (errmess) => ({
    type: ActionTypes.OPERATORS_FAILED,
    payload: errmess
})

export const addOperators = (operators) => ({
    type: ActionTypes.ADD_OPERATORS,
    payload: operators
})
