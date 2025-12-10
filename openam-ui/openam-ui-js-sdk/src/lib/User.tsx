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

import { useEffect, useState } from "react";
import { getConfig } from "./config";
import type { UserService } from "./userService";
import type { UserAuthData, UserData } from "./types";
import { useNavigate } from "react-router";

const config = getConfig();

export type UserProps = {
    userService: UserService;
}

const User: React.FC<UserProps> = ({ userService }) => {
    const navigate = useNavigate();

    const [userAuthData, setUserAuthData] = useState<UserAuthData | null>(null);

    const [userData, setUserData] = useState<UserData | null>(null);
    
    useEffect(() => {
        const initAuth = async () => {
            const newUserAuthData = await userService.getUserIdFromSession()
            if(!newUserAuthData) {
                navigate('/login')
            }
            setUserAuthData(newUserAuthData);
        }
        initAuth();
    }, [])
    
    useEffect(() => {
        if(!userAuthData) {
            return
        }
        const fetchUserData = async () => {
            const data = await userService.getUserData(userAuthData.id, userAuthData.realm);
            setUserData(data);
        }
        fetchUserData();
    }, [userAuthData, userService])

    if (!userData) {
        return <div>Loading user data...</div>; //TODO add customizable loading component
    }

       
    const saveHandler = async () => {
        if (!userData || !userAuthData) {
            return;
        }
        const data = await userService.saveUserData(userAuthData.id, userAuthData.realm, userData);
        setUserData(data);
    };

    const savePassword = async(password: string) => {
        if(!userAuthData) {
            return;
        }
        if(!password) {
            throw new Error("password is empty")
        }

        await userService.savePassword(userAuthData.id, userAuthData.realm, password)
    }

        
    return <config.UserForm userData={userData} setUserData={setUserData} saveHandler={saveHandler} savePasswordHandler={savePassword} />
}

export default User;