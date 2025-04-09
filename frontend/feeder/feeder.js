
// CONSTANTS
// 10.0.0.198:8090
const FEEDER_API = "18.218.44.44:8090"; //18.218.44.44:8090
const VIDEO_FEED_BOX = document.getElementById("video-feed-box");
const VIEWER_COUNT_ELEMENT = document.getElementById("viewer-count")
const LAST_FED_ELEMENT = document.getElementById("last-fed")
const socket = new WebSocket(`ws://${FEEDER_API}`);
let USER_ID;



// ON OPEN, CONNECT (SWITCH TO THE PROPER API)
window.onload = function () {

    try {
        setupWebsocketClient();
        console.log("Successfully connected to:", FEEDER_API);
    
    
    }
    catch (exception) {
        console.log("Couldn't connect to server")
    }

};

window.onclose = function() {

    socket.close();

}

// == OTHER FUNCTIONS ========================================

function setupWebsocketClient() {

    // Setup socket connection.


    // Establish relavant methods.


    socket.onopen = function (event) {

        // socket.send(JSON.stringify({ type: 'ROLE_ASSIGNMENT', role: 'WATCHING_CAT' }));


    };

    socket.onmessage = function (event) {

        let data = JSON.parse(event.data);

        switch (data.type) {


            case "FEEDER_DATA":
                let imageBase64 = data.encodedImage; 
                let viewerCount = data.viewerCount; 
                let lastFedTime = data.formattedLastTimeFed; 
        
                VIDEO_FEED_BOX.style.backgroundImage = "url('data:image/png;base64," + imageBase64 + "')";
                LAST_FED_ELEMENT.innerText = lastFedTime;
                VIEWER_COUNT_ELEMENT.innerText = viewerCount;    
                break;
            case "USER_INIT":
                USER_ID = data.userId;
                console.log("u r new user w/ id: " + USER_ID)
                break;
            default:
                console.log("RECIEVED INVALID JSON TYPE")        

        }

    };



    socket.onclose = function (event) {};
    socket.onerror = function (event) {};

}

function sendFeedRequest() {

    fetch("/feed-request", {method: "POST"});
    console.log("sent request")

    // const message = JSON.stringify({ type: "FEED_REQUEST" });
    // socket.send(message);
}

function backToMainMenu() {
    window.location.href = "/glubest";
}

