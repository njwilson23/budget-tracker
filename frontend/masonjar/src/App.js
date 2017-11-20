import React, { Component } from 'react';
//import logo from './logo.svg';
import './App.css';

class Payment extends Component {
    render() {
        let pmt = this.props.payment;
        let displayString = pmt.payer + " paid " + pmt.payee + " $" + pmt.amount;
        return (
            <div className="payment">
                <p>{displayString}
                    <button className="delete"
                            onClick={this.props.deleteHandler}>Cancel</button>
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

        this.addHandler = props.addHandler;

        this.handleDateChange = this.handleDateChange.bind(this);
        this.handlePayerChange = this.handlePayerChange.bind(this);
        this.handlePayeeChange = this.handlePayeeChange.bind(this);
        this.handleAmountChange = this.handleAmountChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
    }

    handleDateChange(ev) {
        this.setState({"date": ev.target.value});
    }

    handlePayerChange(ev) {
        this.setState({"payer": ev.target.value});
    }

    handlePayeeChange(ev) {
        this.setState({"payee": ev.target.value});
    }

    handleAmountChange(ev) {
        this.setState({"amount": ev.target.value});
    }

    handleSubmit(ev) {
        this.addHandler(this.state)
        ev.preventDefault()
    }

    render() {
        return (
            <form onSubmit={this.handleSubmit}>
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
            "payments": [],
            "host": "http://localhost:8080"
        }

        // Populate payment list
        let req = new XMLHttpRequest();
        req.open("GET", this.state.host + "/payments/all", true);
        req.onreadystatechange = () => {
            if (req.readyState === XMLHttpRequest.DONE && req.status === 200) {
                this.setState({"payments": JSON.parse(req.responseText)});
            }
        }
        req.send();

        this.addPayment = this.addPayment.bind(this)
        this.deletePayment = this.deletePayment.bind(this)
    }

    addPayment(pmt) {
        let req = new XMLHttpRequest();
        req.open("POST", this.state.host + "/payments/add", true);
        req.onreadystatechange = () => {
            if (req.readyState === XMLHttpRequest.DONE && req.status === 200) {
                this.setState({"payments":
                               this.state.payments.concat([[Number(req.responseText), pmt]])});
            }
        }
        req.send(JSON.stringify(pmt));
    }

    deletePayment(id) {
        let req = new XMLHttpRequest();
        req.open("POST", this.state.host + "/payments/delete", true);
        req.onreadystatechange = () => {
            if (req.readyState === XMLHttpRequest.DONE && req.status === 200) {
                this.setState({"payments":
                               this.state.payments.filter((a) => a[0] !== id)});
            }
        }
        req.send(JSON.stringify({"id": id}));
    }

    render() {
        let payments = this.state.payments.map((pmt) =>
            <Payment key={pmt[0]}
                     payment={pmt[1]}
                     deleteHandler={(e) => this.deletePayment(pmt[0])}
             />
        );
        return (
          <div className="App">
            <header className="App-header">
              <h1 className="App-title">Spending Record</h1>
            </header>
            <PaymentEditor host={this.state.host} addHandler={this.addPayment} />
            <div>
              {payments}
            </div>
          </div>
        );
    }
}

export default App;
