# Ideas and 45-minute brainstorming

These are **candidate concepts**, not agreed product decisions. SDK mappings below are hypotheses to test against the documented Vehicle domains and NaviHelper features. No concept has been validated with users.

## Five directions

| Idea and real moment | Smallest useful demo | Dependency and biggest risk |
| --- | --- | --- |
| **Arrival Assist** — a family driving an older passenger to an unfamiliar hospital needs the right entrance and drop-off point. | Choose one of three curated destinations while parked → see an entrance/drop-off card → send its coordinates to navigation → show an arrival checklist when allowed. | NaviHelper destination requests; optional vehicle-state context. Accurate entrance data must be curated; do not claim accessibility verification or live parking availability. |
| **Charge Break** — an EV driver wants something useful to do during a short charging stop. | Read charging state → offer a short saved audio lesson or break routine → pause/resume appropriately when charging or UX restrictions change. | Vehicle EvBattery plus audio focus. Charging callbacks and app category must be tested. Never assume a precise charge-completion time. |
| **Family Stop Planner** — a long trip needs a toilet or short-walk rest stop that suits everyone. | Pick needs and one route preset while parked → compare three curated stops → add the selected waypoint. | NaviHelper waypoints. Amenity data and route suitability are external inputs; the SDK is not a POI database or route optimizer. |
| **Trip Handoff** — a couple changes drivers at a rest area and needs to retain the plan. | Parked summary of route, planned stops, a user-entered reminder, and available fuel/battery data. | NaviHelper plus Vehicle State. Identity and driver-change detection are unverified; MVP uses a manual handoff button. |
| **Journey-sized Learning** — a commuter wants an audio lesson that fits the journey. | Pick a preset topic before departure → play a short bundled audio segment → respond to navigation/audio interruptions → resume later. | NaviHelper route information and audio focus. Trip-duration fields and category approval need confirmation; Pleos voice SDK is not assumed available. |

**Initial recommendation:** test Arrival Assist first; keep Charge Break as the fallback if EV signals work more reliably than navigation. Arrival Assist has a concrete story and can start with three curated places. Charge Break makes direct use of vehicle context. Journey-sized Learning is a good personal-interest option, but must prove its advantage over ordinary phone audio.
For every concept, prepare detailed input and setup while parked and adapt to CarUxRestrictions. Voice, live location, and automatic vehicle controls are stretch dependencies until access is confirmed. Access status · Driving UX

## A 45-minute brainstorming session

1. **0–5 min:** agree the constraints: two people, one core flow, working demo target before submission.
2. **5–15 min:** silently write two recent car frustrations each. Describe who, when, current workaround, and cost in time or effort.
3. **15–25 min:** turn the best problems into four ideas. Ask: what does vehicle context change? Why would someone use it again?
4. **25–35 min:** each independently scores the ideas using the rubric below. Discuss score differences.
5. **35–42 min:** draw three screens for the winner and identify the single riskiest SDK call.
6. **42–45 min:** choose a lead idea, fallback, technical-test owner, and next review date.

## Our simple selection rubric

Score each 1–5: **problem clarity + vehicle advantage + demo feasibility + differentiation + repeat use**. Total /25. These are our decision aids, not official judging weights.
**Hard gate:** reject or shrink an idea if the core permission/API cannot be proved by 2026년 10월 24일. Before final selection, check the Pleos App Market for similar products and record one meaningful difference; novelty is not established by this shortlist.

## Fill in together

- Idea / target user / difficult moment: **TBD**
- Current workaround / why it falls short: **TBD**
- Car-specific advantage: **TBD**
- Three-screen flow: **TBD → TBD → TBD**
- Core SDK dependency / proof result: **TBD**
- Three people to ask / what we learned: **TBD**
- Lead idea / fallback / decision reason: **TBD**
Interview prompt: “Tell me about the last time this happened. What did you do? What was most frustrating?” Show the sketch afterward; ask them to complete the task rather than only asking if they like the idea.