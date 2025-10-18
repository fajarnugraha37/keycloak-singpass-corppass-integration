#!/bin/bash

# Build script for optimized Keycloak custom image
# Usage: ./build.sh [OPTIONS]

set -e

# Default values
IMAGE_NAME="custom-keycloak"
TAG="latest"
DOCKERFILE="Dockerfile"
BUILD_CONTEXT="."
CACHE_FROM=""
PUSH=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--tag)
            TAG="$2"
            shift 2
            ;;
        -n|--name)
            IMAGE_NAME="$2"
            shift 2
            ;;
        -f|--file)
            DOCKERFILE="$2"
            shift 2
            ;;
        --cache-from)
            CACHE_FROM="--cache-from=$2"
            shift 2
            ;;
        --push)
            PUSH=true
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  -t, --tag TAG         Set image tag (default: latest)"
            echo "  -n, --name NAME       Set image name (default: custom-keycloak)"
            echo "  -f, --file FILE       Dockerfile to use (default: Dockerfile)"
            echo "  --cache-from IMAGE    Use cache from specified image"
            echo "  --push                Push image after build"
            echo "  -h, --help            Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

FULL_IMAGE_NAME="${IMAGE_NAME}:${TAG}"

echo "Building Keycloak custom image..."
echo "Image: ${FULL_IMAGE_NAME}"
echo "Dockerfile: ${DOCKERFILE}"
echo "Build context: ${BUILD_CONTEXT}"

# Build the image with buildkit optimizations
DOCKER_BUILDKIT=1 docker build \
    --progress=plain \
    --file "${DOCKERFILE}" \
    --tag "${FULL_IMAGE_NAME}" \
    ${CACHE_FROM} \
    --build-arg BUILDKIT_INLINE_CACHE=1 \
    "${BUILD_CONTEXT}"

echo "Build completed successfully!"

# Push if requested
if [ "$PUSH" = true ]; then
    echo "Pushing image to registry..."
    docker push "${FULL_IMAGE_NAME}"
    echo "Push completed!"
fi

echo "Image: ${FULL_IMAGE_NAME}"
echo "Size: $(docker images --format "table {{.Size}}" "${FULL_IMAGE_NAME}" | tail -1)"