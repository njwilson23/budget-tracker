import React, { Component } from 'react';
//import logo from './logo.svg';
import './App.css';

class Payment extends Component {
    render() {
        return (
            <div className="payment">
                <p>{this.props.payer + " paid " + this.props.payee + " $" + this.props.amount} <button className="delete">X</button>
            </p>
            </div>
        )
    }
}

class PaymentEditor extends Component {

    constructor(props) {
        super(props)
        this.state = {
            "date": "",
            "payer": "",
            "payee": "",
            "amount": 0.00,
            "host": props.host
        }

        this.handleDateChange = this.handleDateChange.bind(this);
        this.handlePayerChange = this.handlePayerChange.bind(this);
        this.handlePayeeChange = this.handlePayeeChange.bind(this);
        this.handleAmountChange = this.handleAmountChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);

    }

    handleDateChange(event) {
        this.setState({"date": event.target.value});
    }

    handlePayerChange(event) {
        this.setState({"payer": event.target.value});
    }

    handlePayeeChange(event) {
        this.setState({"payee": event.target.value});
    }

    handleAmountChange(event) {
        this.setState({"amount": event.target.value});
    }

    handleSubmit(event) {
        console.log(this.state)
        event.preventDefault()
    }

    render() {
        return (<form onSubmit={this.handleSubmit}>
            <label><br/>Date:<input type="text"
                            value={this.state.date}
                            onChange={this.handleDateChange}/></label>
            <label><br/>Payer:<input type="text"
                            value={this.state.payer}
                            onChange={this.handlePayerChange}/></label>
            <label><br/>Payee:<input type="text"
                            value={this.state.payee}
                            onChange={this.handlePayeeChange}/></label>
            <label><br/>Amount:<input type="text"
                            value={this.state.amount}
                            onChange={this.handleAmountChange}/></label>
            <br/><input type="submit" value="Add receipt"/>
            </form>);
    }
}

class App extends Component {

    constructor(props) {
        super(props)
        this.state = {
            "payments": [
                [1, {"payer": "Alice", "payee": "Bob", "amount": 10.00}],
                [2, {"payer": "Bob", "payee": "Alice", "amount": 7.50}],
                [3, {"payer": "Alice", "payee": "Bob", "amount": 19.31}],
            ],
            "host": "localhost:8080"
        }
    }

    render() {
        let payments = this.state.payments.map((pmt) =>
            <Payment key={pmt[0]}
                     payer={pmt[1].payer}
                     payee={pmt[1].payee}
                     amount={pmt[1].amount} />
        );
        return (
          <div className="App">
            <header className="App-header">
              <h1 className="App-title">Spending Record</h1>
            </header>
            <PaymentEditor host={this.state.host} />
            <div>
              {payments}
            </div>
          </div>
        );
    }
}

export default App;
