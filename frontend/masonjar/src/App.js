import React, { Component } from 'react';
import './App.css';

function Payment(props) {

    function fmtMoney(amt) {
        let str = String(amt * 100),
            n = str.length;
        return "$" + str.slice(0, n-2) + "." + str.slice(n-2, n);
    }

    let cls = (props.idx % 2 === 0) ? "payment even" : "payment odd",
        pmt = props.payment,
        displayString = pmt.date + ": " + pmt.payer + " paid " + pmt.payee + " " + fmtMoney(pmt.amount);
    return (
        <div className={cls} onClick={props.onClick}>
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
            "amount": 0.00
        }

        this.values = props.values;

        this.addHandler = props.addHandler;
        this.dateSetter = props.dateSetter;
        this.payeeSetter = props.payeeSetter;
        this.payerSetter = props.payerSetter;
        this.amtSetter = props.amtSetter;

        this.handleSubmit = this.handleSubmit.bind(this);
    }

    handleSubmit(ev) {
        this.addHandler(this.values)
        ev.preventDefault()
    }

    render() {
        return (
            <form onSubmit={this.handleSubmit}>
                <label><br/>Date:
                    <input type="date"
                           value={this.values.date}
                           onChange={(ev) => this.dateSetter(ev.target.value)}/>
                </label>
                <label><br/>Payer:
                    <input type="text"
                           value={this.values.payer}
                           onChange={(ev) => this.payerSetter(ev.target.value)}/>
                </label>
                <label><br/>Payee:
                    <input type="text"
                           value={this.values.payee}
                           onChange={(ev) => this.payeeSetter(ev.target.value)}/>
                </label>
                <label><br/>Amount:
                    <input type="text"
                           value={this.values.amount}
                           onChange={(ev) => this.amtSetter(ev.target.value)}/>
                </label>
                <br/><input type="submit" value="Add receipt"/>
                <br/><button>Settle</button>
            </form>);
    }
}

class App extends Component {

    constructor(props) {
        super(props)
        this.state = {
            "payments": [],
            "host": "http://localhost:8080",
            "editor": {
                "date": "",
                "payer": "",
                "payee": "",
                "amount": 0.0
            }
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
        this.setValues = this.setValues.bind(this)
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

    setValues(pmt) {
        this.setState({"payer": pmt.payer,
                       "payee": pmt.payee,
                       "amount": pmt.amount,
                       "date": pmt.date});
    }

    updateEditor(key, value) {
        let editor = this.state.editor;
        editor[key] = value;
        this.setState({"editor": editor});
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
                     onClick={(e) => this.setValues(pmt[1])}
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
                        values={this.state.editor}
                        dateSetter={(date) => this.updateEditor("date", date)}
                        payerSetter={(payer) => this.updateEditor("payer", payer)}
                        payeeSetter={(payee) => this.updateEditor("payee", payee)}
                        amtSetter={(amt) => this.updateEditor("amount", amt)}
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
