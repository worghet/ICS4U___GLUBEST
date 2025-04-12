// == CONNECTION CONSTANTS ===========================
const FEEDER_API = "10.0.0.198:8090"; //18.218.44.44
let SOCKET;


// == DOM CONSTANTS ==================================
const VIDEO_FEED_BOX = document.getElementById("video-feed-box");
const VIEWER_COUNT_ELEMENT = document.getElementById("viewer-count");
const LAST_FED_ELEMENT = document.getElementById("last-fed");


// == ON LOAD / CLOSE ================================


// This method runs when the page is loaded.
window.onload = function () {
    try {
        // Setup the actual connection.
        setupWebsocketClient();
        console.log("SUCCESSFULLY CONNECTED TO: ", FEEDER_API);
    } catch (exception) {
        console.log("FAILED TO CONNECT TO: ", FEEDER_API);
    }

    // == AUDIO HANDLING =========================
    const audio = new Audio('/resources/glubest.mp3');
    audio.loop = true;
    audio.volume = 0.3;

    audio.addEventListener("loadedmetadata", () => {
        audio.currentTime = 50;
        console.log("Set current time to 50");

        audio.play().then(() => {
            console.log("Started playing from 50s");
        }).catch(err => {
            console.warn("Autoplay may be blocked:", err);
        });
    });

    setInterval(() => {
        localStorage.setItem("glubest_audio_time", audio.currentTime);
    }, 1000);
};




window.onclose = function () {

    // On exiting / closing the page; disconnect the socket.
    SOCKET.close();

}


// == OTHER FUNCTIONS ========================================


function setupWebsocketClient() {

    // Setup socket connection.

    SOCKET = new WebSocket(`ws://${FEEDER_API}`);

    // Setup relevant methods.

    SOCKET.onopen = function (event) { };
    SOCKET.onclose = function (event) { };
    SOCKET.onerror = function (event) { };


    SOCKET.onmessage = function (event) {

        // Unpackage the data.
        let data = JSON.parse(event.data);

        // Identify each segment of the data.
        let imageBase64 = data.encodedImage;
        let viewerCount = data.viewerCount;
        let lastFedTime = data.formattedLastTimeFed;


        // Update corresponding HTML elements.
        VIDEO_FEED_BOX.innerHTML = "";
        VIDEO_FEED_BOX.style.backgroundImage = "url('data:image/png;base64," + imageBase64 + "')";
        VIEWER_COUNT_ELEMENT.innerText = viewerCount;
        LAST_FED_ELEMENT.innerText = lastFedTime;

    };

}

function sendFeedRequest() {

    // Send a simple HTTP request to the server, 
    // which then relays it to the Agent computer.

    fetch("/feed-request", { method: "POST" });

    // Notify console that it has been fulfilled.
    console.log("FEED REQUEST SENT.")
}

function backToMainMenu() {

    // Just changes the location to the main page.
    window.location.href = "/glubest";
}