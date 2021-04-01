FROM cypress/base:13.8.0

ADD . /tmp
WORKDIR /tmp

RUN adduser --disabled-password jahians
RUN chown -R jahians:jahians /tmp
USER jahians

RUN mkdir /tmp/run-artifacts
RUN mkdir /tmp/results

#CI=true reduces the verbosity of the installation logs
RUN CI=true yarn install
RUN CI=true /tmp/node_modules/.bin/cypress install

CMD ["/bin/bash", "-c", "/tmp/env.run.sh"]
