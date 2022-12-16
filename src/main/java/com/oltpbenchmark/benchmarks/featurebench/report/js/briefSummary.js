const accordion = document.querySelectorAll(".accordion-box .label");
console.log(accordion);
for (i = 0; i < accordion.length; i++) {
    accordion[i].addEventListener("click", function () {
        this.parentNode.classList.toggle("active");
    });
}

const hamburger = document.getElementById("hamburger");
const sideBar = document.getElementById("sideBar");
hamburger.addEventListener("click", () => {
    sideBar.style.width = "100vw";
    sideBar.style.transition = "1s";
});
const closeSideBar = document.getElementById("closeSideBar");
closeSideBar.addEventListener("click", () => {
    sideBar.style.width = "0";
    sideBar.style.transition = "0.2s";
});

// const ctx = document.getElementById("briefSummaryVertical");

// new Chart(ctx, {
//     type: "bar",
//     data: {
//         labels: [
//             "RangeScan(PG)",
//             "RangeScan(YB)",
//             "RangeScan(Aurora)",
//             "IndexScan(PG)",
//             "IndexScan(YB)",
//             "IndexScan(Aurora)",
//         ],
//         datasets: [
//             {
//                 label: "# Client Side Latency (ms)",
//                 data: [0.65, 1.2, 0.85, 0.55, 1, 0.65],

//                 backgroundColor: [
//                     "#60a5fa",
//                     "#60a5fa",
//                     "#60a5fa",
//                     "#818cf8",
//                     "#818cf8",
//                     "#818cf8",
//                 ],
//                 borderRadius: 10,
//             },
//         ],
//     },
//     options: {
//         maintainAspectRatio: false,
//         scales: {
//             y: {
//                 beginAtZero: true,
//             },
//         },
//         plugins: {
//             title: {
//                 display: true,
//                 text: "RangeScan vs IndexScan",
//             },
//         },
//     },
// });

// const ctx2 = document.getElementById("briefSummaryHorizontal");

// new Chart(ctx2, {
//     type: "bar",
//     data: {
//         labels: [
//             "RangeScan(PG)",
//             "RangeScan(YB)",
//             "RangeScan(Aurora)",
//             "IndexScan(PG)",
//             "IndexScan(YB)",
//             "IndexScan(Aurora)",
//         ],
//         datasets: [
//             {
//                 label: "# Client Side Latency (ms)",
//                 data: [0.65, 1.2, 0.85, 0.55, 1, 0.65],

//                 backgroundColor: [
//                     "#60a5fa",
//                     "#60a5fa",
//                     "#60a5fa",
//                     "#818cf8",
//                     "#818cf8",
//                     "#818cf8",
//                 ],
//                 borderRadius: 10,
//                 barThickness: 20,
//             },
//         ],
//     },
//     options: {
//         indexAxis: "y",
//         maintainAspectRatio: false,
//         scales: {
//             y: {
//                 beginAtZero: true,
//             },
//         },
//         plugins: {
//             title: {
//                 display: true,
//                 text: "RangeScan vs IndexScan",
//             },
//         },
//     },
// });

const avergaeChart = document.getElementById("avergaeChart");
const avergaeChart2 = document.getElementById("avergaeChart2");
new Chart(avergaeChart, {
    type: "bar",
    data: {
        labels: ["PG", "YB", "Aurora"],
        datasets: [
            {
                label: "# Client Side Latency (ms)",
                data: [0.65, 1.2, 0.85],

                backgroundColor: ["#60a5fa", "#60a5fa", "#60a5fa"],
                borderRadius: 10,
            },
        ],
    },
    options: {
        //  indexAxis: 'y',
        maintainAspectRatio: false,
        scales: {
            y: {
                beginAtZero: true,
            },
        },
        plugins: {
            title: {
                display: true,
                text: "Average Client Side Latency ",
            },
        },
    },
});

new Chart(avergaeChart2, {
    type: "bar",
    data: {
        labels: ["PG", "YB", "Aurora"],
        datasets: [
            {
                label: "# Client Side Latency (ms)",
                data: [0.65, 1.2, 0.85],

                backgroundColor: ["#60a5fa", "#60a5fa", "#60a5fa"],
                borderRadius: 10,
            },
        ],
    },
    options: {
        //  indexAxis: 'y',
        maintainAspectRatio: false,
        scales: {
            y: {
                beginAtZero: true,
            },
        },
        plugins: {
            title: {
                display: true,
                text: "Average Client Side Latency ",
            },
        },
    },
});
