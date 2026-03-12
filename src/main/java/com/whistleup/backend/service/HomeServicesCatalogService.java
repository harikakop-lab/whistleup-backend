package com.whistleup.backend.service;

import com.whistleup.backend.resource.HomeServiceCategoryResource;
import com.whistleup.backend.resource.HomeServiceOptionResource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HomeServicesCatalogService {

    public List<HomeServiceCategoryResource> getCatalog() {
        return List.of(
                HomeServiceCategoryResource.builder()
                        .key("plumber")
                        .label("Plumber")
                        .subtitle("Professional plumbing services at your doorstep.")
                        .icon("tools")
                        .options(List.of(
                                HomeServiceOptionResource.builder()
                                        .id("tap-repair")
                                        .title("Tap / Faucet Repair")
                                        .description("Fix leaky or broken taps and faucets quickly.")
                                        .price(299)
                                        .popular(true)
                                        .build(),
                                HomeServiceOptionResource.builder()
                                        .id("drain-unclogging")
                                        .title("Drain Unclogging")
                                        .description("Kitchen and bathroom drainage blockage removal.")
                                        .price(399)
                                        .build()
                        ))
                        .build(),
                HomeServiceCategoryResource.builder()
                        .key("carpenter")
                        .label("Carpentry")
                        .subtitle("Professional carpentry services at your doorstep.")
                        .icon("hammer")
                        .options(List.of(
                                HomeServiceOptionResource.builder()
                                        .id("door-hinge")
                                        .title("Door Hinge Repair")
                                        .description("Repair for squeaky or damaged doors and hinges.")
                                        .price(99)
                                        .popular(true)
                                        .build(),
                                HomeServiceOptionResource.builder()
                                        .id("drawer-fix")
                                        .title("Drawer & Cabinet Fix")
                                        .description("Smooth slide correction and fitting replacement.")
                                        .price(249)
                                        .build()
                        ))
                        .build(),
                HomeServiceCategoryResource.builder()
                        .key("electrician")
                        .label("Electrician")
                        .subtitle("Professional electric services at your doorstep.")
                        .icon("flash")
                        .options(List.of(
                                HomeServiceOptionResource.builder()
                                        .id("fan-installer")
                                        .title("Fan Installer")
                                        .description("Installation of ceiling fans and regulator issues.")
                                        .price(99)
                                        .popular(true)
                                        .build(),
                                HomeServiceOptionResource.builder()
                                        .id("switch-board")
                                        .title("Switch Board Repair")
                                        .description("Fix faulty switches, sockets and wiring points.")
                                        .price(149)
                                        .build()
                        ))
                        .build(),
                HomeServiceCategoryResource.builder()
                        .key("cleaner")
                        .label("Cleaner")
                        .subtitle("Professional home cleaning services at your doorstep.")
                        .icon("broom")
                        .options(List.of(
                                HomeServiceOptionResource.builder()
                                        .id("bedroom-deep-clean")
                                        .title("Bedroom Deep Clean")
                                        .description("Complete sanitization and deep scrubbing.")
                                        .price(599)
                                        .popular(true)
                                        .build(),
                                HomeServiceOptionResource.builder()
                                        .id("full-home-deep-clean")
                                        .title("Full Home Deep Clean")
                                        .description("Includes living room, bedrooms, kitchen and bathrooms.")
                                        .price(1499)
                                        .build()
                        ))
                        .build(),
                HomeServiceCategoryResource.builder()
                        .key("painter")
                        .label("Painter")
                        .subtitle("Professional fresh coats to rooms or even flats.")
                        .icon("roller")
                        .options(List.of(
                                HomeServiceOptionResource.builder()
                                        .id("full-flat-painting")
                                        .title("Full Flat Painting")
                                        .description("Room prep, primer and two-coat finish.")
                                        .price(3599)
                                        .popular(true)
                                        .build(),
                                HomeServiceOptionResource.builder()
                                        .id("accent-wall")
                                        .title("Accent Wall Painting")
                                        .description("Single wall highlight with premium finish options.")
                                        .price(999)
                                        .build()
                        ))
                        .build(),
                HomeServiceCategoryResource.builder()
                        .key("beautician")
                        .label("Beautician")
                        .subtitle("Professional relaxation just for you.")
                        .icon("face-woman")
                        .options(List.of(
                                HomeServiceOptionResource.builder()
                                        .id("basic-facial")
                                        .title("Basic Facial")
                                        .description("At-home skin cleanup and glow facial.")
                                        .price(899)
                                        .popular(true)
                                        .build(),
                                HomeServiceOptionResource.builder()
                                        .id("spa-package")
                                        .title("Home Spa Package")
                                        .description("Relaxing massage and skincare package.")
                                        .price(1599)
                                        .build()
                        ))
                        .build()
        );
    }
}
