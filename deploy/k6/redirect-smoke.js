import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 20,
  duration: "30s",
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<200"],
  },
};

const BASE = __ENV.BASE_URL || "http://localhost:8088";
const CODE = __ENV.SHORT_CODE || "smoke1";

export default function () {
  const res = http.get(`${BASE}/${CODE}`, { redirects: 0 });
  check(res, {
    "redirect or missing": (r) => r.status === 302 || r.status === 404,
  });
  sleep(0.2);
}
