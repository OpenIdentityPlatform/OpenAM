/**
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2025 3A Systems LLC.
 */

import { useState } from "react";
import type { UserData } from "../types";
import type { UserForm } from "./types";

const DefaultUserForm: UserForm = ({ userData, setUserData, saveHandler, savePasswordHandler }) => {

    const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false);

    //return // Helper to handle string/array fields
    const handleChange = (key: keyof UserData, value: string) => {
        setUserData({ ...userData, [key]: [value] });
    };


    return (
        <>
            <h2>User Profile</h2>
            <form
                onSubmit={e => {
                    e.preventDefault();
                    saveHandler();
                }}>
                <div className="form-group">
                    <label htmlFor="username">Username:</label>
                    <input id="username"
                        name="username"
                        type="text"
                        value={userData.username} readOnly={true}
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="givenName">First Name:</label>
                    <input id="givenName"
                        name="givenName"
                        type="text"
                        value={userData.givenName ? userData.givenName[0] : ""}
                        onChange={e => handleChange("givenName", e.target.value)}
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="sn">Last Name:</label>
                    <input id="sn"
                        name="sn"
                        type="text"
                        value={userData.sn ? userData.sn[0] : ""}
                        onChange={e => handleChange("sn", e.target.value)}
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="mail">Mail:</label>
                    <input id="mail"
                        name="mail"
                        type="email"
                        value={userData.mail ? userData.mail[0] : ""}
                        onChange={e => handleChange("mail", e.target.value)}
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="telephoneNumber">Phone number:</label>
                    <input id="telephoneNumber"
                        name="telephoneNumber"
                        type="tel"
                        value={userData.telephoneNumber ? userData.telephoneNumber[0] : ""}
                        onChange={e => handleChange("telephoneNumber", e.target.value)}
                    />
                </div>
                <div
                    className="change-password-link"
                    onClick={() => setIsPasswordModalOpen(true)}
                    style={{ marginBottom: "1.5rem" }} // Inline style to ensure spacing from button
                >
                    Change Password
                </div>
                <div className="button-group">
                    <input className="primary-button" type="submit" value="Save" />
                </div>
            </form>
            {/* Change Password Modal */}
            {isPasswordModalOpen && <ChangePasswordModal setIsPasswordModalOpen={setIsPasswordModalOpen} savePasswordHandler={savePasswordHandler} />}
        </>);
}

const ChangePasswordModal = ({ setIsPasswordModalOpen, savePasswordHandler }: {
    setIsPasswordModalOpen: (isPasswordModalOpen: boolean) => void
    savePasswordHandler: (password: string) => void

}) => {
    const [passwordData, setPasswordData] = useState({
        newPassword: "",
        confirmPassword: ""
    });

    // Helper for password fields
    const handlePasswordChange = (key: string, value: string) => {
        setPasswordData({ ...passwordData, [key]: value });
    };

    // Handler for password submission
    const handlePasswordSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (passwordData.newPassword !== passwordData.confirmPassword) {
            alert("Passwords do not match.");
            return;
        }
        
        try {
            await savePasswordHandler(passwordData.newPassword)
            alert("password saved")
            setIsPasswordModalOpen(false);            
        } catch(err) {
            let message: string = ""
            if (typeof err === "string") {
                message = err
            } else if (err instanceof Error) {
                message = err.message 
            }
            alert(`Error saving password: ${message}`)
        }
        setPasswordData({ newPassword: "", confirmPassword: "" });
    };

    return <div className="modal-overlay">
        <div className="modal-content">
            <h3>Change Password</h3>
            <form style={{ width: '100%' }} onSubmit={handlePasswordSubmit}>

                <div className="form-group">
                    <label htmlFor="newPassword">New:</label>
                    <input
                        id="newPassword"
                        type="password"
                        value={passwordData.newPassword}
                        onChange={(e) => handlePasswordChange("newPassword", e.target.value)}
                        required
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="confirmPassword">Confirm:</label>
                    <input
                        id="confirmPassword"
                        type="password"
                        value={passwordData.confirmPassword}
                        onChange={(e) => handlePasswordChange("confirmPassword", e.target.value)}
                        required
                    />
                </div>
                <div className="button-group">
                    <button
                        type="button"
                        className="secondary-button"
                        onClick={() => setIsPasswordModalOpen(false)}
                    >
                        Cancel
                    </button>
                    <button type="submit" className="primary-button">
                        Update Password
                    </button>
                </div>
            </form>
        </div>
    </div>
}

export default DefaultUserForm;    