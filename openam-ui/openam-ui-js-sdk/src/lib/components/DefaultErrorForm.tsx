import type { ErrorForm } from "./types";

const DefaultErrorForm: ErrorForm = ({ error, resetError }) => {
    return <div>
        <h2>An error occurred</h2>
        <p>{error?.message}</p>
        <div className="button-group">
            <input type="button" className="primary-button" value="Retry" onClick={() => resetError()} />
        </div>
    </div>
}

export default DefaultErrorForm;