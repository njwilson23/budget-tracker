import React, { Component } from 'react';
//import logo from './logo.svg';
import './App.css';

function Payment(props) {
    let cls = (props.idx % 2 === 0) ? "payment even" : "payment odd",
        pmt = props.payment,
        displayString = pmt.date + ": " + pmt.payer + " paid " + pmt.payee + " $" + pmt.amount;
    return (
        <div className={cls}>
            <div className="paymentString">{displayString}
                <span className="flexSpace"></span>
            </div>
            <div className="paymentDelete" onClick={props.deleteHandler}>{"Cancel"}</div>
        </div>
    )
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
        let owes = "(Not sure who owes what)";
        return (
            <form onSubmit={this.handleSubmit}>
                <label><br/>Date:<input type="date"
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
                <br/><p>{owes}</p>
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
        let payments = this.state.payments.slice()
        payments.sort(
            (a, b) => (a[1].date < b[1].date) ? -1 : ((a[1].date > b[1].date) ? 1 : 0)
        );
        let sortedPayments = payments.map((pmt, idx) =>
            <Payment key={pmt[0]}
                     idx={idx}
                     payment={pmt[1]}
                     deleteHandler={(e) => this.deletePayment(pmt[0])}
             />
        );
        return (
          <div className="App">
            <header className="App-header">
              <h1 className="App-title">Household Spending</h1>
            </header>
            <div className="container">
                <div className="editorContainer">
                    <PaymentEditor
                        host={this.state.host}
                        addHandler={this.addPayment} />
                </div>
                <div className="paymentList">
                  {sortedPayments}
                </div>
            </div>
          </div>
        );
    }
}

export default App;
