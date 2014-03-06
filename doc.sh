TRAVIS_REPO_SLUG=grails-plugins/grails-cache-ehcache
TRAVIS_BUILD_ID=20220372
GIT_NAME=Craig Andrews
GIT_EMAIL=candrews@integralblue.com
GIT_TOKEN=b25c145b92657f247a737902ade2528e487f4ae9

rm -rf docs
grails doc --pdf

git config user.name $GIT_NAME
git config user.email $GIT_EMAIL
git config credential.helper "store --file=.git/credentials"
echo "https://$GH_TOKEN:@github.com" > .git/credentials
git clone https://github.com:$TRAVIS_REPO_SLUG.git -b gh-pages gh-pages --single-branch
cd gh-pages
git rm -rf *
cp -r ../docs .
git add *
git commit -a -m "Updating docs for Travis build: https://travis-ci.org/$TRAVIS_REPO_SLUG/builds/$TRAVIS_BUILD_ID"
git push origin HEAD
cd ..
rm -rf gh-pages
