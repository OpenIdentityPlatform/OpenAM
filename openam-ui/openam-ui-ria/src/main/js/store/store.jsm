import { createStore, compose, combineReducers } from "redux";
import { setRealm } from "./reducers";

const defaultState = {};
const rootReducer = combineReducers({ setRealm });
const enhancers = compose(window.devToolsExtension ? window.devToolsExtension() : (f) => f);

const store = createStore(rootReducer, defaultState, enhancers);

export default store;
