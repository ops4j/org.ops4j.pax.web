Working release prepare

mvn -Prelease,repos.sonatype.staging -Darguments="-Prelease,repos.sonatype.staging" release:prepare -DautoVersionSubmodules=true

push changes

git push
git push --tag

Working release perform

mvn -Prelease,repos.sonatype.staging -Darguments="-Prelease,repos.sonatype.staging" -Dgoals=deploy release:perform


go to oss.sonatype.org and push pax-web to central