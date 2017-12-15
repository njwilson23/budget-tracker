import React, { Component } from 'react';
import './App.css';

function Payment(props) {

    let fmtMoney = amt => {
        let str = String(Math.round(amt * 100, 0)),
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
            <div className="paymentDelete button" onClick={props.deleteHandler}>{"Cancel"}</div>
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
        </form><button onClick={props.settle}>Settle</button></div>);
}

function MonthBrowser(props) {
    return (
        <div className="monthBrowser">
            <div className="navButton button" onClick={props.prev}>Previous month</div>
            <div className="navButton button" onClick={props.next}>Next month</div>
        </div>
    );
}

function fmtDate(dt) {
    let lpad = a => "0".repeat(Math.max(0, 2 - String(a).length)) + String(a)
    let result = String(dt.getFullYear()) +
            "-" + lpad(dt.getMonth()+1) +
            "-" + lpad(dt.getDate());
    return result;
}

function prevMonth(dt) {
    const yr = dt.getFullYear(),
          mo = dt.getMonth();
    return new Date((mo === 0) ? yr - 1 : yr,
                    (mo === 0) ? 12 : mo - 1,
                    1);
}

function nextMonth(dt) {
    const yr = dt.getFullYear(),
          mo = dt.getMonth();
    return new Date((mo === 11) ? yr + 1 : yr,
                    (mo === 11) ? 0 : mo + 1,
                    1);
}

function firstOfMonth(dt) {
    return new Date(dt.getFullYear(), dt.getMonth(), 1);
}

function lastOfMonth(dt) {
    return new Date(dt.getFullYear(), dt.getMonth() + 1, 0);
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
            },
            "year": currentDate.getFullYear(),
            "month": currentDate.getMonth()
        }

        this.addPayment = this.addPayment.bind(this);
        this.deletePayment = this.deletePayment.bind(this);
        this.updateEditor = this.updateEditor.bind(this);
        this.changeMonth = this.changeMonth.bind(this);
        this.getPayments = this.getPayments.bind(this);
        this.getOwed = this.getOwed.bind(this);

        this.getPayments(currentDate);
    }

    getPayments(dt) {
        // Populate payment list
        let req = new XMLHttpRequest();
        req.open("GET", this.state.host +
                    "/payments?after=" +
                    fmtDate(lastOfMonth(prevMonth(dt)))+
                    "&before=" +
                    fmtDate(firstOfMonth(nextMonth(dt))), true);
        req.onreadystatechange = () => {
            if (req.readyState === XMLHttpRequest.DONE && req.status === 200) {
                this.setState({"payments": JSON.parse(req.responseText)});
            }
        }
        req.send();
    }

    changeMonth(direction) {
        let newMonth = this.state.month + direction,
            newYear = this.state.year;
        if (newMonth === -1) {
            newMonth = 11;
            newYear--
        } else if (newMonth === 12) {
            newMonth = 0;
            newYear++
        }
        this.setState({
            "month": newMonth,
            "year": newYear
        });
        this.getPayments(new Date(newYear, newMonth, 1));
    }

    addPayment(pmt) {
        let req = new XMLHttpRequest();
        req.open("POST", this.state.host + "/payments/add", true);
        req.onreadystatechange = () => {
            if (req.readyState === XMLHttpRequest.DONE && req.status === 200) {
                let pmtId = Number(req.responseText);
                let pmtCopy = Object.assign({}, pmt);
                this.setState(
                    {"payments": this.state.payments.concat([[pmtId, pmtCopy]])}
                );
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

    getOwed(debtor, lender) {
        // Populate payment list
        let req = new XMLHttpRequest();
        req.open("GET", this.state.host +
                    "/payments/owed?from=" + debtor +
                    "&to=" + lender, true);
        req.onreadystatechange = () => {
            if (req.readyState === XMLHttpRequest.DONE && req.status === 200) {
                console.log(req.responseText);
            }
        }
        req.send();
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
            <MonthBrowser prev={() => this.changeMonth(-1)}
                          next={() => this.changeMonth(1)}
            />
          </div>
        );
    }
}

export default App;
