export function setRealm (state = "", action) {
    switch (action.type) {
        case "SET_REALM":
            return action.absolutePath;
        default:
            return state;
    }
}
