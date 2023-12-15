import matsim
import pandas as pd
import random

plans = matsim.plan_reader('plan_drt3.0.xml',  True)
import datetime
import time

toWork = pd.read_csv("toWork.csv")
toWorkTime = list(toWork["x"])
toWorkProb = list(toWork["y"])
toHome = pd.read_csv("toHome.csv")
toHomeTime = list(toHome["x"])
toHomeProb = list(toHome["y"])


def timechange(end_time):
    x = time.strptime(f'{end_time},000'.split(',')[0],'%H:%M:%S')
    return datetime.timedelta(hours=x.tm_hour,minutes=x.tm_min,seconds=x.tm_sec).total_seconds()

with open("output/drtPlan.xml", 'wb+') as f_write:
    writer = matsim.writers.PopulationWriter(f_write)

    writer.start_population()
    for person, plan in plans:

        #filter out extreme e
        work_activities = filter(
            lambda e: e.tag == 'activity' and e.attrib['type'] == 'work',
            plan)
        work_activities = list(work_activities)[0]

        home_activities = filter(
            lambda e: e.tag == 'activity' and e.attrib['type'] == 'home',
            plan)
        home_activities = list(home_activities)[0]

        home_x = float(home_activities.attrib["x"])
        home_y = float(home_activities.attrib["y"])

        work_x = float(work_activities.attrib["x"])
        work_y = float(work_activities.attrib["y"])

        x = ((home_x - work_x) ** 2 + (home_y - work_y) ** 2) ** (1 / 2)
        if x > 30000: continue

        writer.start_person(person.attrib['id'])
        writer.start_plan(True)

        #randomized in time bin
        homeEnd = random.choices(toWorkTime, weights=toWorkProb,k=1)[0]*60*60
        homeEnd += random.randint(0,599)
        workStart = homeEnd
        workEnd = random.choices(toHomeTime, weights=toHomeProb,k=1)[0]*60*60
        workEnd += random.randint(0, 599)

        #print(home_x, home_y, homeEnd)

        writer.add_activity(type='home', x=home_x, y=home_y, end_time=homeEnd)
        writer.add_leg(mode='drt')
        writer.add_activity(type='work', x=work_x, y=work_y, start_time=workStart, end_time=workEnd)
        writer.add_leg(mode='drt')
        writer.add_activity(type='home', x=home_x, y=home_y)

        writer.end_plan()
        writer.end_person()

    writer.end_population()
    