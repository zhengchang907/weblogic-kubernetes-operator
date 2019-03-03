// Copyright 2019, Oracle Corporation and/or its affiliates. All rights reserved.

import React, { Component } from 'react'
import Main from './components/Main'
import './App.css'
import { BrowserRouter } from 'react-router-dom'
import { library } from '@fortawesome/fontawesome-svg-core'
import { fab, faGithub } from '@fortawesome/free-brands-svg-icons'
import { faBookOpen, faInfo, faHome, faGlobe, faQuestionCircle } from '@fortawesome/free-solid-svg-icons'

library.add(fab, faBookOpen, faGithub, faInfo, faHome, faGlobe, faQuestionCircle)

class App extends Component {
  render() {
    return (
      <BrowserRouter>
        <div>
          <Main />
        </div>
      </BrowserRouter>
    );
  }
}

export default App;
