
// CONSTANTS
const FEEDER_API = "127.0.0.1:8000";
const VIDEO_FEED_BOX = document.getElementById("video-feed-box");


// ON OPEN, CONNECT (SWITCH TO THE PROPER API)
window.onload = function () {

    try {
        console.log(VIDEO_FEED_BOX)
        connectToAPI(FEEDER_API);
        console.log("Successfully connected to:", FEEDER_API);
    }
    catch (exception) {
        console.log("Couldn't connect to server")
    }

};


// == OTHER FUNCTIONS ========================================

function connectToAPI(apiUrl) {

    // Setup socket connection.
    const socket = new WebSocket(`ws://${apiUrl}`);


    // Establish relavant methods.


    socket.onopen = function (event) {
        socket.send("I am a connected client!"); // Example message
    };

    socket.onmessage = function (event) {
        console.log(event.data)

        // repeatedly (smoothly), update the image.
        requestAnimationFrame(function() {
            VIDEO_FEED_BOX.style.backgroundImage = "url('data:image/png;base64," + event.data + "')";
            VIDEO_FEED_BOX.style.backgroundSize = "cover";
            VIDEO_FEED_BOX.style.backgroundPosition = "center";
        });

    };



    socket.onclose = function (event) {};
    socket.onerror = function (event) {};

}