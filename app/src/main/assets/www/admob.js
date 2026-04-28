/**
 * KidZone Professional Bridge System
 * SOLID: Interface Segregation & Dependency Inversion
 */
window.AdMobMgr = {
    // Get current settings
    lang: localStorage.getItem("kz-lang") || "uz",

    init: function(gameId) {
        console.log("Game Engine Started: " + gameId);
        this.gameId = gameId;
        // Notify parent
        this._post({type: 'ready', id: gameId});
    },

    // Save score and check record
    saveResult: function(score) {
        const key = "kz-best-" + this.gameId;
        const currentBest = parseInt(localStorage.getItem(key) || "0");
        let isNewRecord = false;

        if (score > currentBest) {
            localStorage.setItem(key, score);
            isNewRecord = true;
        }

        this._post({
            type: 'addPts',
            amount: Math.round(score / 2),
            isRecord: isNewRecord,
            score: score
        });

        // Show Interstitial after significant progress
        if (score > 10) this.showInterstitial();
    },

    showInterstitial: function() {
        this._post({type: 'showInterstitial'});
    },

    showRewarded: function() {
        this._post({type: 'showRewarded'});
    },

    _post: function(data) {
        if (window.parent && window.parent.postMessage) {
            window.parent.postMessage(data, '*');
        }
    }
};

// Global Listener for Rewards
window.onRewardGranted = function(amount) {
    window.dispatchEvent(new CustomEvent("rewardGranted", {detail: {amount: amount}}));
};
