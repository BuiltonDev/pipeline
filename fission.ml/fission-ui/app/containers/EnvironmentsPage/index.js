/*
 *
 * EnvironmentsPage
 *
 */

import React, { PropTypes } from 'react';
import { FormattedMessage } from 'react-intl';
import messages from './messages';

export class EnvironmentsPage extends React.Component { // eslint-disable-line react/prefer-stateless-function

  render() {
    return (
      <div className="col-md-12">
        <h1>
          <FormattedMessage {...messages.header} />
        </h1>
        {React.Children.toArray(this.props.children)}
      </div>
    );
  }
}

EnvironmentsPage.propTypes = {
  children: PropTypes.node,
};


export default EnvironmentsPage;
