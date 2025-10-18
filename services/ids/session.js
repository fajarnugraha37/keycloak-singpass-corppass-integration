export const sessionStore = {
    // kc_sid -> Set of app token jti
    map: new Map(),
    revokedJti: new Set(),

    /**
     * Links a Keycloak session ID to an application token JTI.
     * @param {[string]} kc_sid 
     * @param {string} jti 
     * @returns {void}
     */
    link(kc_sid, jti) {
        if (!kc_sid) 
            return;
        if (!this.map.has(kc_sid)) 
            this.map.set(kc_sid, new Set());
        
        this.map.get(kc_sid).add(jti);
    },

    /**
     * Revokes all application tokens linked to a Keycloak session ID.
     * @param {string} kc_sid 
     * @returns {void}
     */
    revokeBySid(kc_sid) {
        const set = this.map.get(kc_sid);
        if (set) {
            for (const jti of set) this.revokedJti.add(jti);
            this.map.delete(kc_sid);
        }
    },

    /**
     * Checks if an application token JTI is revoked.
     * @param {string} jti 
     * @returns {boolean}
     */
    isRevoked(jti) {
        return this.revokedJti.has(jti);
    }
};