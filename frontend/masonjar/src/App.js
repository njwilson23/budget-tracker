import React, { Component } from 'react';
import './App.css';

function Payment(props) {

    let fmtMoney = amt => {
        let str = String(amt * 100),
            n = str.length;
        return "$" + str.slice(0, n-2) + "." + str.slice(n-2, n)
    };

    let fmtPayment = pmt => pmt.date + ": " + pmt.payer +
                              " paid " + pmt.payee +
                              " " + fmtMoney(pmt.amount);

    const cls = (props.idx % 2 === 0) ? "payment even" : "payment odd",
          displayString = fmtPayment(props.payment);

    return (
        <div className={cls} onClick={props.onClick}>
            <div className="paymentString">{displayString}
                <span className="flexSpace"></span>
            </div>
            <div className="paymentDelete" onClick={props.deleteHandler}>{"Cancel"}</div>
        </div>
    )
}

function PaymentList(props) {

    props.payments.sort(
        ([_ida, a], [_idb, b]) =>
            (a.date < b.date) ? -1 : ((a.date > b.date) ? 1 : 0)
    );
    let sortedPayments = props.payments.map(([pmtId, pmt], idx) =>
        <Payment key={pmtId}
                 idx={idx}
                 payment={pmt}
                 deleteHandler={e => props.deletePayment(pmtId)}
                 onClick={e => props.onClick(pmt)}
         />
    );

    return <div className="paymentList">{sortedPayments}</div>
}

function PaymentEditor(props) {

    let handleSubmit = ev => {
        props.post(props.getValues());
        ev.preventDefault();
    }


    return (
        <div className="editorContainer"><form onSubmit={handleSubmit}>
            <label><br/>Date:
                <input type="date"
                       value={props.values.date}
                       onChange={ev => props.dateSetter(ev.target.value)}/>
            </label>
            <label><br/>Payer:
                <input type="text"
                       value={props.values.payer}
                       onChange={ev => props.payerSetter(ev.target.value)}/>
            </label>
            <label><br/>Payee:
                <input type="text"
                       value={props.values.payee}
                       onChange={ev => props.payeeSetter(ev.target.value)}/>
            </label>
            <label><br/>Amount:
                <input type="text"
                       value={props.values.amount}
                       onChange={ev => props.amtSetter(ev.target.value)}/>
            </label>
            <br/><input type="submit" value="Add receipt"/>
            <br/><button>Settle</button>
        </form></div>);

}

function fmtDate(dt) {
    let lpad = a => "0".repeat(Math.max(0, 2 - String(a).length)) + String(a)
    return String(dt.getYear() + 1900) +
            "-" + lpad(dt.getMonth()+1) +
            "-" + lpad(dt.getDate());
}

class App extends Component {

    constructor(props) {
        super(props)
        let currentDate = new Date();

        this.state = {
            "payments": [],
            "host": "http://localhost:8080",
            "editor": {
                "date": fmtDate(currentDate),
                "payer": "",
                "payee": "",
                "amount": 0.0
            }
        }

        this.addPayment = this.addPayment.bind(this);
        this.deletePayment = this.deletePayment.bind(this);
        this.updateEditor = this.updateEditor.bind(this);

        // Populate payment list
        let req = new XMLHttpRequest();
        req.open("GET", this.state.host + "/payments/all", true);
        req.onreadystatechange = () => {
            if (req.readyState === XMLHttpRequest.DONE && req.status === 200) {
                this.setState({"payments": JSON.parse(req.responseText)});
            }
        }
        req.send();
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
                               this.state.payments.filter(
                                   ([pmtId, _]) => pmtId !== id
                               )});
            }
        }
        req.send(JSON.stringify({"id": id}));
    }

    updateEditor(key, value) {
        let editor = this.state.editor;
        editor[key] = value;
        this.setState({"editor": editor});
    }

    render() {
        return (
          <div className="App">
            <header className="App-header">
              <h1 className="App-title">Household Spending</h1>
            </header>
            <div className="flexContainer">
                <PaymentEditor
                    values={this.state.editor}
                    dateSetter={date => this.updateEditor("date", date)}
                    payerSetter={payer => this.updateEditor("payer", payer)}
                    payeeSetter={payee => this.updateEditor("payee", payee)}
                    amtSetter={amt => this.updateEditor("amount", amt)}
                    getValues={() => this.state.editor}
                    post={this.addPayment}
                />
                <PaymentList payments={this.state.payments.slice()}
                             deletePayment={this.deletePayment}
                             onClick={pmt => this.setState({"editor":
                                        {"date": pmt.date,
                                         "payer": pmt.payer,
                                         "payee": pmt.payee,
                                         "amount": pmt.amount}})}
                />
            </div>
          </div>
        );
    }
}

export default App;
